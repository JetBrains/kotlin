/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.debugger.evaluate

import org.jetbrains.jet.plugin.debugger.evaluate.*
import com.intellij.psi.PsiElement
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.*
import com.intellij.debugger.engine.evaluation.expression.*
import org.jetbrains.jet.lang.resolve.AnalyzingUtils
import org.jetbrains.jet.codegen.state.GenerationState
import org.jetbrains.jet.codegen.ClassBuilderFactories
import java.util.Collections
import org.jetbrains.jet.codegen.KotlinCodegenFacade
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.jet.plugin.JetLanguage
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.Opcodes.ASM5
import org.jetbrains.org.objectweb.asm.*
import com.intellij.openapi.util.Computable
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.jet.plugin.refactoring.createTempCopy
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionData
import org.jetbrains.jet.plugin.refactoring.extractFunction.performAnalysis
import org.jetbrains.jet.plugin.refactoring.extractFunction.validate
import org.jetbrains.jet.plugin.refactoring.extractFunction.generateFunction
import org.jetbrains.jet.lang.psi.JetNamedFunction
import com.intellij.psi.PsiFile
import org.jetbrains.jet.codegen.ClassFileFactory
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils
import org.jetbrains.jet.OutputFileCollection
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.lang.psi.JetImportList
import org.jetbrains.jet.lang.psi.codeFragmentUtil.skipVisibilityCheck
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.Status
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.codegen.CompilationErrorHandler
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.ErrorMessage
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages
import com.sun.jdi.request.EventRequest
import com.sun.jdi.ObjectReference
import com.intellij.debugger.engine.SuspendContext
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionOptions

object KotlinEvaluationBuilder: EvaluatorBuilder {
    override fun build(codeFragment: PsiElement, position: SourcePosition?): ExpressionEvaluator {
        if (codeFragment !is JetCodeFragment || position == null) {
            return EvaluatorBuilderImpl.getInstance()!!.build(codeFragment, position)
        }

        val file = position.getFile()
        if (file !is JetFile) {
            throw EvaluateExceptionUtil.createEvaluateException("Couldn't evaluate kotlin expression in non-kotlin context")
        }

        val packageName = file.getPackageDirective()?.getFqName()?.asString()
        if (packageName != null) {
            codeFragment.addImportsFromString("import $packageName.*")
        }
        return ExpressionEvaluatorImpl(KotlinEvaluator(codeFragment as JetCodeFragment, position))
    }
}

val logger = Logger.getInstance(javaClass<KotlinEvaluator>())

class KotlinEvaluator(val codeFragment: JetCodeFragment,
                      val sourcePosition: SourcePosition
) : Evaluator {
    override fun evaluate(context: EvaluationContextImpl): Any? {
        try {
            val extractedFunction = getFunctionForExtractedFragment(codeFragment, sourcePosition.getFile(), sourcePosition.getLine())
            if (extractedFunction == null) {
                throw IllegalStateException("Code fragment cannot be extracted to function: ${sourcePosition.getFile().getText()}:${sourcePosition.getLine()},\ncodeFragment = ${codeFragment.getText()}")
            }

            val classFileFactory = createClassFileFactory(extractedFunction)

            // KT-4509
            val outputFiles = (classFileFactory : OutputFileCollection).asList().filter { it.relativePath != "$packageInternalName.class" }
            if (outputFiles.size() != 1) exception("Expression compiles to more than one class file. Note that lambdas, classes and objects are unsupported yet. List of files: ${outputFiles.makeString(",")}")

            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            var resultValue: Value? = null
            ClassReader(outputFiles.first().asByteArray()).accept(object : ClassVisitor(ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    if (name == extractedFunction.getName()) {
                        return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                            override fun visitEnd() {
                                val value = interpreterLoop(
                                        this,
                                        makeInitialFrame(this, context.getArgumentsByNames(extractedFunction.getParameterNamesForDebugger())),
                                        JDIEval(virtualMachine,
                                                context.getClassLoader()!!,
                                                context.getSuspendContext().getThread()?.getThreadReference()!!, context.getSuspendContext().getInvokePolicy())
                                )

                                resultValue = when (value) {
                                    is ValueReturned -> value.result
                                    is ExceptionThrown -> exception(value.exception.toString())
                                    is AbnormalTermination -> exception(value.message)
                                    else -> throw IllegalStateException("Unknown result value produced by eval4j")
                                }
                            }
                        }
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions)
                }
            }, 0)

            if (resultValue == null) {
                throw IllegalStateException("resultValue is null: cannot find method ${extractedFunction.getName()} in ${outputFiles.first().relativePath}")
            }

            return resultValue!!.asJdiValue(virtualMachine, resultValue!!.asmType)
        }
        catch(e: EvaluateException) {
            throw e
        }
        catch (e: Exception) {
            logger.error("Couldn't evaluate expression:\nfileText = ${sourcePosition.getFile().getText()}\nline = ${sourcePosition.getLine()}\ncodeFragment = ${codeFragment.getText()}", e)
            val cause = if (e.getMessage() != null) ": ${e.getMessage()}" else ""
            exception("An exception occurs during Evaluate Expression Action $cause")
        }
    }

    override fun getModifier(): Modifier? {
        return null
    }

    private fun SuspendContext.getInvokePolicy(): Int {
        return if (getSuspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD) ObjectReference.INVOKE_SINGLE_THREADED else 0
    }

    private fun JetNamedFunction.getParameterNamesForDebugger(): List<String> {
        val result = arrayListOf<String>()
        if (getReceiverTypeRef() != null) {
            result.add("this")
        }
        for (param in getValueParameters()) {
            result.add(param.getName()!!)
        }
        return result
    }

    private fun EvaluationContextImpl.getArgumentsByNames(parameterNames: List<String>): List<Value> {
        val frames = getFrameProxy()?.getStackFrame()
        if (frames != null) {
            fun getValue(name: String): Value {
                return try {
                    when (name) {
                        "this" -> frames.thisObject().asValue()
                        else -> frames.getValue(frames.visibleVariableByName(name)).asValue()
                    }
                }
                catch(e: Exception) {
                    exception("Cannot get parameter value from local variables table: parameterName = ${name}. Note that captured parameters are unsupported yet.")
                }
            }

            return parameterNames.map { getValue(it) }
        }
        return Collections.emptyList()
    }

    private fun createClassFileFactory(extractedFunction: JetNamedFunction): ClassFileFactory {
        return ApplicationManager.getApplication()?.runReadAction(object: Computable<ClassFileFactory> {
            override fun compute(): ClassFileFactory? {
                val file = createFileForDebugger(codeFragment, extractedFunction)

                checkForSyntacticErrors(file)

                val analyzeExhaust = file.getAnalysisResults()
                if (analyzeExhaust.isError()) {
                    exception(analyzeExhaust.getError())
                }

                val bindingContext = analyzeExhaust.getBindingContext()
                bindingContext.getDiagnostics().forEach {
                    diagnostic ->
                    if (diagnostic.getSeverity() == Severity.ERROR) {
                        exception(DefaultErrorMessages.RENDERER.render(diagnostic))
                    }
                }

                val state = GenerationState(
                        file.getProject(),
                        ClassBuilderFactories.BINARIES,
                        analyzeExhaust.getModuleDescriptor(),
                        bindingContext,
                        listOf(file)
                )

                KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)

                return state.getFactory()
            }
        })!!

    }

    private fun exception(msg: String) = throw EvaluateExceptionUtil.createEvaluateException(msg)

    private fun exception(e: Throwable) {
        val message = e.getMessage()
        if (message != null) {
            exception(message)
        }
        throw EvaluateExceptionUtil.createEvaluateException(e)
    }
}

private val template = """
package packageForDebugger

!IMPORT_LIST!

!FUNCTION!
"""

private val packageInternalName = PackageClassUtils.getPackageClassFqName(FqName("packageForDebugger")).asString().replace(".", "/")

private fun createFileForDebugger(codeFragment: JetCodeFragment,
                                  extractedFunction: JetNamedFunction
): JetFile {
    var fileText = template.replace("!IMPORT_LIST!",
                                    codeFragment.importsToString()
                                            .split(JetCodeFragment.IMPORT_SEPARATOR)
                                            .makeString("\n"))

    val extractedFunctionText = extractedFunction.getText()
    assert(extractedFunctionText != null, "Text of extracted function shouldn't be null")
    fileText = fileText.replace("!FUNCTION!", extractedFunction.getText()!!)

    val virtualFile = LightVirtualFile("debugFile.kt", JetLanguage.INSTANCE, fileText)
    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
    val jetFile = (PsiFileFactory.getInstance(codeFragment.getProject()) as PsiFileFactoryImpl)
            .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false) as JetFile
    jetFile.skipVisibilityCheck = true
    return jetFile
}

fun addImportsToFile(newImportList: JetImportList?, tmpFile: JetFile) {
    if (newImportList != null) {
        val tmpFileImportList = tmpFile.getImportList()
        val packageDirective = tmpFile.getPackageDirective()
        if (tmpFileImportList == null) {
            tmpFile.addAfter(JetPsiFactory.createNewLine(tmpFile.getProject()), packageDirective)
            tmpFile.addAfter(newImportList, tmpFile.getPackageDirective())
        }
        else {
            tmpFileImportList.replace(newImportList)
        }
        tmpFile.addAfter(JetPsiFactory.createNewLine(tmpFile.getProject()), packageDirective)
    }
}

fun addDebugExpressionBeforeContextElement(codeFragment: JetCodeFragment, contextElement: PsiElement): JetExpression? {
    val parent = contextElement.getParent()
    if (parent == null) return null

    parent.addBefore(JetPsiFactory.createNewLine(contextElement.getProject()), contextElement)

    val debugExpression = codeFragment.getContentElement()
    if (debugExpression == null) return null

    val newDebugExpression = parent.addBefore(debugExpression, contextElement)
    if (newDebugExpression == null) return null

    parent.addBefore(JetPsiFactory.createNewLine(contextElement.getProject()), contextElement)

    return newDebugExpression as JetExpression
}

fun checkForSyntacticErrors(file: JetFile) {
    try {
        AnalyzingUtils.checkForSyntacticErrors(file)
    }
    catch (e: IllegalArgumentException) {
        throw EvaluateExceptionUtil.createEvaluateException(e.getMessage())
    }
}

private fun getFunctionForExtractedFragment(
        codeFragment: JetCodeFragment,
        breakpointFile: PsiFile,
        breakpointLine: Int
): JetNamedFunction? {

    fun getErrorMessageForExtractFunctionResult(analysisResult: AnalysisResult): String {
        return analysisResult.messages.map {
            errorMessage ->
            val message = when(errorMessage) {
                ErrorMessage.NO_EXPRESSION -> "Cannot perform an action without an expression"
                ErrorMessage.NO_CONTAINER -> "Cannot perform an action at this breakpoint ${breakpointFile.getName()}:${breakpointLine}"
                ErrorMessage.SUPER_CALL -> "Cannot perform an action for expression with super call"
                ErrorMessage.DENOTABLE_TYPES -> "Cannot perform an action because following types are unavailable from debugger scope"
                ErrorMessage.MULTIPLE_OUTPUT -> "Cannot perform an action because this code fragment changes more than one variable"
                ErrorMessage.DECLARATIONS_OUT_OF_SCOPE,
                ErrorMessage.OUTPUT_AND_EXIT_POINT,
                ErrorMessage.MULTIPLE_EXIT_POINTS,
                ErrorMessage.VARIABLES_ARE_USED_OUTSIDE -> "Cannot perform an action for this expression"
            }
            if (errorMessage.additionalInfo == null) message else "$message: ${errorMessage.additionalInfo?.makeString(", ")}"
        }.makeString(", ")
    }

    return ApplicationManager.getApplication()?.runReadAction(object: Computable<JetNamedFunction> {
        override fun compute(): JetNamedFunction? {
            checkForSyntacticErrors(codeFragment)

            val originalFile = breakpointFile as JetFile

            val lineStart = CodeInsightUtils.getStartLineOffset(originalFile, breakpointLine)
            if (lineStart == null) return null

            val tmpFile = originalFile.createTempCopy { it }
            tmpFile.skipVisibilityCheck = true

            val elementAtOffset = tmpFile.findElementAt(lineStart)
            if (elementAtOffset == null) return null

            val contextElement: PsiElement = CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart) ?: elementAtOffset

            addImportsToFile(codeFragment.importsAsImportList(), tmpFile)

            val newDebugExpression = addDebugExpressionBeforeContextElement(codeFragment, contextElement)
            if (newDebugExpression == null) return null

            val targetSibling = tmpFile.getDeclarations().firstOrNull()
            if (targetSibling == null) return null

            val analysisResult = ExtractionData(
                    tmpFile, Collections.singletonList(newDebugExpression), targetSibling, ExtractionOptions(false)
            ).performAnalysis()
            if (analysisResult.status != Status.SUCCESS) {
                throw EvaluateExceptionUtil.createEvaluateException(getErrorMessageForExtractFunctionResult(analysisResult))
            }

            val validationResult = analysisResult.descriptor!!.validate()
            if (!validationResult.conflicts.isEmpty()) {
                throw EvaluateExceptionUtil.createEvaluateException("Following declarations are unavailable in debug scope: ${validationResult.conflicts.keySet()?.map { it.getText() }?.makeString(",")}")
            }

            return validationResult.descriptor.generateFunction(true)
        }
    })
}

