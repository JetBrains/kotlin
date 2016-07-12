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

package org.jetbrains.kotlin.cli.common.messages

import com.intellij.openapi.util.io.FileUtil.toSystemDependentName
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiFormatUtil
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTrackerImpl
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.sortedDiagnostics
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.load.java.components.IncompatibleVersionErrorData
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.jvm.JvmBindingContextSlices
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import java.util.*

class AnalyzerWithCompilerReport(private val messageCollector: MessageCollector) {
    lateinit var analysisResult: AnalysisResult

    private fun reportIncompleteHierarchies() {
        val bindingContext = analysisResult.bindingContext
        val classes = bindingContext.getKeys(TraceBasedErrorReporter.INCOMPLETE_HIERARCHY)
        if (!classes.isEmpty()) {
            val message = StringBuilder("Supertypes of the following classes cannot be resolved. " +
                                        "Please make sure you have the required dependencies in the classpath:\n")
            for (descriptor in classes) {
                val fqName = DescriptorUtils.getFqName(descriptor).asString()
                val unresolved = bindingContext.get(TraceBasedErrorReporter.INCOMPLETE_HIERARCHY, descriptor)
                assert(unresolved != null && !unresolved.isEmpty()) { "Incomplete hierarchy should be reported with names of unresolved superclasses: " + fqName }
                message.append("    class ").append(fqName).append(", unresolved supertypes: ").append(unresolved!!.joinToString()).append("\n")
            }
            messageCollector.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION)
        }
    }

    private fun reportAlternativeSignatureErrors() {
        val bc = analysisResult.bindingContext
        val descriptorsWithErrors = bc.getKeys(JvmBindingContextSlices.LOAD_FROM_JAVA_SIGNATURE_ERRORS)
        if (!descriptorsWithErrors.isEmpty()) {
            val message = StringBuilder("The following Java entities have annotations with wrong Kotlin signatures:\n")
            for (descriptor in descriptorsWithErrors) {
                val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
                assert(declaration is PsiModifierListOwner)

                val errors = bc.get(JvmBindingContextSlices.LOAD_FROM_JAVA_SIGNATURE_ERRORS, descriptor)
                assert(errors != null && !errors.isEmpty())

                val externalName = PsiFormatUtil.getExternalName(declaration as PsiModifierListOwner)
                message.append(externalName).append(":\n")

                for (error in errors!!) {
                    message.append("    ").append(error).append("\n")
                }
            }
            messageCollector.report(CompilerMessageSeverity.ERROR, message.toString(), CompilerMessageLocation.NO_LOCATION)
        }
    }

    private val abiVersionErrors: List<IncompatibleVersionErrorData>
        get() {
            val bindingContext = analysisResult.bindingContext

            val errorClasses = bindingContext.getKeys(TraceBasedErrorReporter.METADATA_VERSION_ERRORS)
            val result = ArrayList<IncompatibleVersionErrorData>(errorClasses.size)
            for (kotlinClass in errorClasses) {
                result.add(bindingContext.get(TraceBasedErrorReporter.METADATA_VERSION_ERRORS, kotlinClass)!!)
            }

            return result
        }

    private fun reportMetadataVersionErrors(errors: List<IncompatibleVersionErrorData>) {
        for (data in errors) {
            reportIncompatibleBinaryVersion(messageCollector, data, JvmMetadataVersion.INSTANCE, "metadata", CompilerMessageSeverity.ERROR)
        }
    }

    private fun reportSyntaxErrors(files: Collection<KtFile>) {
        for (file in files) {
            reportSyntaxErrors(file, messageCollector)
        }
    }

    class SyntaxErrorReport(val isHasErrors: Boolean, val isAllErrorsAtEof: Boolean)

    fun hasErrors(): Boolean {
        return messageCollector.hasErrors()
    }

    interface Analyzer {
        fun analyze(): AnalysisResult

        fun reportEnvironmentErrors() {
        }
    }

    fun analyzeAndReport(files: Collection<KtFile>, analyzer: Analyzer) {
        analysisResult = analyzer.analyze()
        reportSyntaxErrors(files)
        if (analysisResult.bindingContext.diagnostics.any { it.isValid && it.severity == Severity.ERROR }) {
            analyzer.reportEnvironmentErrors()
        }
        val abiVersionErrors = abiVersionErrors
        reportDiagnostics(analysisResult.bindingContext.diagnostics, messageCollector, !abiVersionErrors.isEmpty())
        if (hasErrors()) {
            reportMetadataVersionErrors(abiVersionErrors)
        }
        reportIncompleteHierarchies()
        reportAlternativeSignatureErrors()
    }

    private class MyDiagnostic<E : PsiElement>(psiElement: E, factory: DiagnosticFactory0<E>,
                                               val message: String) : SimpleDiagnostic<E>(psiElement, factory, Severity.ERROR) {

        override fun isValid(): Boolean = true
    }

    companion object {

        fun convertSeverity(severity: Severity): CompilerMessageSeverity = when (severity) {
            Severity.INFO -> CompilerMessageSeverity.INFO
            Severity.ERROR -> CompilerMessageSeverity.ERROR
            Severity.WARNING -> CompilerMessageSeverity.WARNING
            else -> throw IllegalStateException("Unknown severity: " + severity)
        }

        private val SYNTAX_ERROR_FACTORY = DiagnosticFactory0.create<PsiErrorElement>(Severity.ERROR)

        private fun reportDiagnostic(
                diagnostic: Diagnostic,
                reporter: DiagnosticMessageReporter,
                incompatibleFilesFound: Boolean): Boolean {
            if (!diagnostic.isValid) return false

            var render: String = if (diagnostic is MyDiagnostic<*>) {
                diagnostic.message
            }
            else {
                DefaultErrorMessages.render(diagnostic)
            }

            if (incompatibleFilesFound && diagnostic.factory in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS) {
                render += "\n(note: this may be caused by the fact that some classes compiled with an incompatible version of Kotlin " +
                          "were found in the classpath. Such classes cannot be loaded properly by this version of Kotlin compiler. " +
                          "See below for more information)"
            }

            val file = diagnostic.psiFile
            reporter.report(diagnostic, file, render)

            return diagnostic.severity == Severity.ERROR
        }

        fun reportDiagnostics(
                diagnostics: Diagnostics,
                reporter: DiagnosticMessageReporter,
                incompatibleFilesFound: Boolean): Boolean {
            var hasErrors = false
            for (diagnostic in sortedDiagnostics(diagnostics.all())) {
                hasErrors = hasErrors or reportDiagnostic(diagnostic, reporter, incompatibleFilesFound)
            }
            return hasErrors
        }

        fun reportDiagnostics(
                diagnostics: Diagnostics,
                messageCollector: MessageCollector,
                incompatibleFilesFound: Boolean): Boolean {
            return reportDiagnostics(diagnostics, DefaultDiagnosticReporter(messageCollector), incompatibleFilesFound)
        }

        fun reportDiagnostics(diagnostics: Diagnostics, messageCollector: MessageCollector): Boolean {
            return reportDiagnostics(diagnostics, DefaultDiagnosticReporter(messageCollector), false)
        }

        fun reportSyntaxErrors(
                file: PsiElement,
                reporter: DiagnosticMessageReporter): SyntaxErrorReport {
            class ErrorReportingVisitor : AnalyzingUtils.PsiErrorElementVisitor() {
                var hasErrors = false
                var allErrorsAtEof = true

                private fun <E : PsiElement> reportDiagnostic(element: E, factory: DiagnosticFactory0<E>, message: String) {
                    val diagnostic = MyDiagnostic(element, factory, message)
                    AnalyzerWithCompilerReport.reportDiagnostic(diagnostic, reporter, false)
                    if (element.textRange.startOffset != file.textRange.endOffset) {
                        allErrorsAtEof = false
                    }
                    hasErrors = true
                }

                override fun visitErrorElement(element: PsiErrorElement) {
                    val description = element.errorDescription
                    reportDiagnostic(element, SYNTAX_ERROR_FACTORY,
                                     if (StringUtil.isEmpty(description)) "Syntax error" else description)
                }
            }

            val visitor = ErrorReportingVisitor()

            file.accept(visitor)

            return SyntaxErrorReport(visitor.hasErrors, visitor.allErrorsAtEof)
        }

        fun reportSyntaxErrors(file: PsiElement, messageCollector: MessageCollector): SyntaxErrorReport {
            return reportSyntaxErrors(file, DefaultDiagnosticReporter(messageCollector))
        }

        fun reportBytecodeVersionErrors(bindingContext: BindingContext, messageCollector: MessageCollector) {
            val severity = if (System.getProperty("kotlin.jvm.disable.bytecode.version.error") == "true")
                CompilerMessageSeverity.WARNING
            else
                CompilerMessageSeverity.ERROR

            val locations = bindingContext.getKeys(IncompatibleClassTrackerImpl.BYTECODE_VERSION_ERRORS)
            if (locations.isEmpty()) return

            for (location in locations) {
                val data = bindingContext.get(IncompatibleClassTrackerImpl.BYTECODE_VERSION_ERRORS, location)
                           ?: error("Value is missing for key in binding context: " + location)
                reportIncompatibleBinaryVersion(messageCollector, data, JvmBytecodeBinaryVersion.INSTANCE, "bytecode", severity)
            }
        }

        private fun reportIncompatibleBinaryVersion(
                messageCollector: MessageCollector,
                data: IncompatibleVersionErrorData,
                expectedVersion: BinaryVersion,
                versionSortText: String,
                severity: CompilerMessageSeverity) {
            messageCollector.report(
                    severity,
                    "Class '" + JvmClassName.byClassId(data.classId) + "' was compiled with an incompatible version of Kotlin. " +
                    "The binary version of its " + versionSortText + " is " + data.actualVersion + ", " +
                    "expected version is " + expectedVersion,
                    CompilerMessageLocation.create(toSystemDependentName(data.filePath), -1, -1, null)
            )
        }

    }
}
