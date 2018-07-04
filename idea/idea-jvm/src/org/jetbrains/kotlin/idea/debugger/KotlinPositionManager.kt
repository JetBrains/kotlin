/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.MultiRequestPositionManager
import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.PositionManagerEx
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ThreeState
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.Method
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest
import org.jetbrains.kotlin.codegen.inline.KOTLIN_STRATA_NAME
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.KotlinFileTypeFactory
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.breakpoints.getLambdasAtLineIfAny
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import com.intellij.debugger.engine.DebuggerUtils as JDebuggerUtils

class KotlinPositionManager(private val myDebugProcess: DebugProcess) : MultiRequestPositionManager, PositionManagerEx() {
    private val allKotlinFilesScope =
            object : DelegatingGlobalSearchScope(
                    KotlinSourceFilterScope.projectAndLibrariesSources(GlobalSearchScope.allScope(myDebugProcess.project), myDebugProcess.project)
            ) {
                private val projectIndex = ProjectRootManager.getInstance(myDebugProcess.project).fileIndex
                private val scopeComparator =
                        Comparator.comparing(projectIndex::isInSourceContent)
                                .thenComparing(projectIndex::isInLibrarySource)
                                .thenComparing { file1, file2 -> super.compare(file1, file2) }

                override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
                    return scopeComparator.compare(file1, file2)
                }
            }

    private val sourceSearchScopes: List<GlobalSearchScope> = listOf(
            myDebugProcess.searchScope,
            allKotlinFilesScope
    )

    override fun getAcceptedFileTypes(): Set<FileType> = KotlinFileTypeFactory.KOTLIN_FILE_TYPES_SET

    override fun evaluateCondition(context: EvaluationContext, frame: StackFrameProxyImpl, location: Location, expression: String): ThreeState? {
        return ThreeState.UNSURE
    }

    override fun createStackFrame(frame: StackFrameProxyImpl, debugProcess: DebugProcessImpl, location: Location): XStackFrame? {
        if (location.declaringType().containsKotlinStrata()) {
            return KotlinStackFrame(frame)
        }
        return null
    }

    override fun getSourcePosition(location: Location?): SourcePosition? {
        if (location == null) throw NoDataException.INSTANCE

        val fileName = location.safeSourceName ?: throw NoDataException.INSTANCE
        val lineNumber = location.safeLineNumber()
        if (lineNumber < 0) {
            throw NoDataException.INSTANCE
        }

        if (!DebuggerUtils.isKotlinSourceFile(fileName)) throw NoDataException.INSTANCE

        val psiFile = getPsiFileByLocation(location)?.let {
            replaceWithAlternativeSource(it, location)
        }

        if (psiFile == null) {
            val isKotlinStrataAvailable = location.declaringType().containsKotlinStrata()
            if (isKotlinStrataAvailable) {
                try {
                    val javaSourceFileName = location.sourceName("Java")
                    val javaClassName = JvmClassName.byInternalName(defaultInternalName(location))
                    val project = myDebugProcess.project

                    val defaultPsiFile = DebuggerUtils.findSourceFileForClass(project, sourceSearchScopes, javaClassName, javaSourceFileName)
                    if (defaultPsiFile != null) {
                        return SourcePosition.createFromLine(defaultPsiFile, 0)
                    }
                }
                catch (e: AbsentInformationException) {
                    // ignored
                }
            }

            throw NoDataException.INSTANCE
        }

        if (psiFile !is KtFile) throw NoDataException.INSTANCE

        val sourceLineNumber = location.safeSourceLineNumber()
        if (sourceLineNumber < 0) {
            throw NoDataException.INSTANCE
        }

        val lambdaOrFunIfInside = getLambdaOrFunIfInside(location, psiFile, sourceLineNumber)
        if (lambdaOrFunIfInside != null) {
            return SourcePosition.createFromElement(lambdaOrFunIfInside.bodyExpression!!)
        }

        val elementInDeclaration = getElementForDeclarationLine(location, psiFile, sourceLineNumber)
        if (elementInDeclaration != null) {
            return SourcePosition.createFromElement(elementInDeclaration)
        }

        if (sourceLineNumber > psiFile.getLineCount() && myDebugProcess.isDexDebug()) {
            val (line, ktFile) = ktLocationInfo(location, true, myDebugProcess.project, false, psiFile)
            return SourcePosition.createFromLine(ktFile ?: psiFile, line - 1)
        }

        val sameLineLocations = location.safeMethod()?.safeAllLineLocations()?.filter {
            it.safeLineNumber() == lineNumber && it.safeSourceName == fileName
        }

        if (sameLineLocations != null) {
            // There're several locations for same source line. If same source position would be created for all of them,
            // breakpoints at this line will stop on every location.
            // Each location is probably some code in arguments between inlined invocations (otherwise same line locations would
            // have been merged into one), but it's impossible to correctly map locations to actual source expressions now.
            val locationIndex = sameLineLocations.indexOf(location)
            if (locationIndex > 0) {
                /*
                    `finally {}` block code is placed in the class file twice.
                    Unless the debugger metadata is available, we can't figure out if we are inside `finally {}`, so we have to check it using PSI.
                    This is conceptually wrong and won't work in some cases, but it's still better than nothing.
                */
                val elementAt = psiFile.getLineStartOffset(lineNumber)?.let { psiFile.findElementAt(it) }
                val isInsideDuplicatedFinally = elementAt != null && elementAt.getStrictParentOfType<KtFinallySection>() != null
                if (!isInsideDuplicatedFinally) {
                    return KotlinReentrantSourcePosition(SourcePosition.createFromLine(psiFile, sourceLineNumber))
                }
            }
        }

        return SourcePosition.createFromLine(psiFile, sourceLineNumber)
    }

    class KotlinReentrantSourcePosition(delegate: SourcePosition) : DelegateSourcePosition(delegate)

    private fun replaceWithAlternativeSource(psiFile: PsiFile, location: Location): PsiFile {
        fun findAlternativeSource(): PsiFile? {
            val qName = location.declaringType().name()
            val alternativeFileUrl = DebuggerUtilsEx.getAlternativeSourceUrl(qName, myDebugProcess.project) ?: return null
            val alternativePsiFile = VirtualFileManager.getInstance().findFileByUrl(alternativeFileUrl) ?: return null
            return psiFile.manager.findFile(alternativePsiFile)
        }

        return findAlternativeSource() ?: psiFile
    }

    // Returns a property or a constructor if debugger stops at class declaration
    private fun getElementForDeclarationLine(location: Location, file: KtFile, lineNumber: Int): KtElement? {
        val lineStartOffset = file.getLineStartOffset(lineNumber) ?: return null
        val elementAt = file.findElementAt(lineStartOffset)
        val contextElement = KotlinCodeFragmentFactory.getContextElement(elementAt)

        if (contextElement !is KtClass) return null

        val methodName = location.method().name()
        return when {
            JvmAbi.isGetterName(methodName) -> {
                val parameterForGetter = contextElement.primaryConstructor?.valueParameters?.firstOrNull {
                    it.hasValOrVar() && it.name != null && JvmAbi.getterName(it.name!!) == methodName
                } ?: return null
                parameterForGetter
            }
            methodName == "<init>" -> contextElement.primaryConstructor
            else -> null
        }
    }

    private fun getLambdaOrFunIfInside(location: Location, file: KtFile, lineNumber: Int): KtFunction? {
        val currentLocationFqName = location.declaringType().name() ?: return null

        val start = CodeInsightUtils.getStartLineOffset(file, lineNumber)
        val end = CodeInsightUtils.getEndLineOffset(file, lineNumber)
        if (start == null || end == null) return null

        val literalsOrFunctions = getLambdasAtLineIfAny(file, lineNumber)
        if (literalsOrFunctions.isEmpty()) return null

        val elementAt = file.findElementAt(start) ?: return null
        val typeMapper = KotlinDebuggerCaches.getOrCreateTypeMapper(elementAt)

        val currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(FqName(currentLocationFqName))
                .internalName.replace('/', '.')

        for (literal in literalsOrFunctions) {
            if (InlineUtil.isInlinedArgument(literal, typeMapper.bindingContext, true)) {
                if (isInsideInlineArgument(literal, location, myDebugProcess as DebugProcessImpl)) {
                    return literal
                }
                continue
            }

            val internalClassNames = DebuggerClassNameProvider(myDebugProcess, alwaysReturnLambdaParentClass = false)
                    .getOuterClassNamesForElement(literal.firstChild)
                    .classNames

            if (internalClassNames.any { it == currentLocationClassName }) {
                return literal
            }
        }

        return null
    }

    private val Location.safeSourceName: String? get() {
        return try {
            sourceName()
        }
        catch (e: AbsentInformationException) {
            null
        }
        catch (e: InternalError) {
            null
        }
    }

    private fun getPsiFileByLocation(location: Location): PsiFile? {
        val sourceName = location.safeSourceName ?: return null

        val referenceInternalName = try {
            if (location.declaringType().containsKotlinStrata()) {
                //replace is required for windows
                location.sourcePath().replace('\\', '/')
            }
            else {
                defaultInternalName(location)
            }
        }
        catch (e: AbsentInformationException) {
            defaultInternalName(location)
        }

        val className = JvmClassName.byInternalName(referenceInternalName)

        val project = myDebugProcess.project

        return DebuggerUtils.findSourceFileForClass(project, sourceSearchScopes, className, sourceName)
    }

    private fun defaultInternalName(location: Location): String {
        //no stratum or source path => use default one
        val referenceFqName = location.declaringType().name()
        // JDI names are of form "package.Class$InnerClass"
        return referenceFqName.replace('.', '/')
    }

    override fun getAllClasses(sourcePosition: SourcePosition): List<ReferenceType> {
        val psiFile = sourcePosition.file
        if (psiFile is KtFile) {
            if (!ProjectRootsUtil.isInProjectOrLibSource(psiFile)) return emptyList()
            return DebuggerClassNameProvider(myDebugProcess).getClassesForPosition(sourcePosition)
        }

        if (psiFile is ClsFileImpl) {
            val decompiledPsiFile = psiFile.readAction { it.decompiledPsiFile }
            if (decompiledPsiFile is KtClsFile && sourcePosition.line == -1) {
                val className = JvmFileClassUtil.getFileClassInternalName(decompiledPsiFile)
                return myDebugProcess.virtualMachineProxy.classesByName(className)
            }
        }

        throw NoDataException.INSTANCE
    }

    fun originalClassNamesForPosition(position: SourcePosition): List<String> {
        return DebuggerClassNameProvider(myDebugProcess, findInlineUseSites = false).getOuterClassNamesForPosition(position)
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }
        try {
            if (myDebugProcess.isDexDebug()) {
                val inlineLocations = runReadAction { getLocationsOfInlinedLine(type, position, myDebugProcess.searchScope) }
                if (!inlineLocations.isEmpty()) {
                    return inlineLocations
                }
            }

            val line = position.line + 1

            val locations = type.locationsOfLine(KOTLIN_STRATA_NAME, null, line)
            if (locations == null || locations.isEmpty()) {
                throw NoDataException.INSTANCE
            }

            return locations.filter { it.sourceName(KOTLIN_STRATA_NAME) == position.file.name }
        }
        catch (e: AbsentInformationException) {
            throw NoDataException.INSTANCE
        }
    }

    @Deprecated("Since Idea 14.0.3 use createPrepareRequests fun")
    override fun createPrepareRequest(classPrepareRequestor: ClassPrepareRequestor, sourcePosition: SourcePosition): ClassPrepareRequest? {
        return createPrepareRequests(classPrepareRequestor, sourcePosition).firstOrNull()
    }

    override fun createPrepareRequests(requestor: ClassPrepareRequestor, position: SourcePosition): List<ClassPrepareRequest> {
        if (position.file !is KtFile) {
            throw NoDataException.INSTANCE
        }

        return DumbService.getInstance(myDebugProcess.project).runReadActionInSmartMode(Computable {
            val classNames = DebuggerClassNameProvider(myDebugProcess).getOuterClassNamesForPosition(position)
            classNames.flatMap { name ->
                listOfNotNull(
                        myDebugProcess.requestsManager.createClassPrepareRequest(requestor, name),
                        myDebugProcess.requestsManager.createClassPrepareRequest(requestor, "$name$*")
                )
            }
        })
    }

    private fun ReferenceType.containsKotlinStrata() = availableStrata().contains(KOTLIN_STRATA_NAME)
}

inline fun <U, V> U.readAction(crossinline f: (U) -> V): V {
    return runReadAction { f(this) }
}
