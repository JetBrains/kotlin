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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiFormatUtil
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.SyntaxErrorReporter.SyntaxErrorReport
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.sortedDiagnostics
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.checkers.OptInUsageChecker
import org.jetbrains.kotlin.resolve.jvm.JvmBindingContextSlices

@K1Deprecation
class AnalyzerWithCompilerReport(private val configuration: CompilerConfiguration) : AbstractAnalyzerWithCompilerReport {
    override val targetEnvironment: TargetEnvironment
        get() = CompilerEnvironment

    override lateinit var analysisResult: AnalysisResult

    @OptIn(MessageCollectorAccess::class) // K1
    private val messageCollector = configuration.messageCollector

    private fun reportIncompleteHierarchies() {
        val bindingContext = analysisResult.bindingContext
        val classes = bindingContext.getKeys(TraceBasedErrorReporter.INCOMPLETE_HIERARCHY)
        if (!classes.isEmpty()) {
            val message = StringBuilder(
                "Supertypes of the following classes cannot be resolved. " +
                        "Please make sure you have the required dependencies in the classpath:\n"
            )
            for (descriptor in classes) {
                val fqName = DescriptorUtils.getFqName(descriptor).asString()
                val unresolved = bindingContext.get(TraceBasedErrorReporter.INCOMPLETE_HIERARCHY, descriptor)
                assert(unresolved != null && !unresolved.isEmpty()) {
                    "Incomplete hierarchy should be reported with names of unresolved superclasses: $fqName"
                }
                message.append("    class ").append(fqName)
                    .append(", unresolved supertypes: ").append(unresolved!!.joinToString())
                    .append("\n")
            }
            messageCollector.report(ERROR, message.toString())
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
            messageCollector.report(ERROR, message.toString())
        }
    }

    private fun reportSyntaxErrors(files: Collection<KtFile>) {
        for (file in files) {
            reportSyntaxErrors(file, messageCollector)
        }
    }

    override fun hasErrors(): Boolean = CheckDiagnosticCollector.checkHasErrors(configuration)

    override fun analyzeAndReport(files: Collection<KtFile>, analyze: () -> AnalysisResult) {
        analysisResult = analyze()
        if (!analysisResult.isError()) {
            OptInUsageChecker.checkCompilerArguments(
                analysisResult.moduleDescriptor, configuration.languageVersionSettings,
                reportError = { message -> messageCollector.report(ERROR, message) },
                reportWarning = { message -> messageCollector.report(WARNING, message) }
            )
        }
        reportSyntaxErrors(files)
        reportDiagnostics(analysisResult.bindingContext.diagnostics, messageCollector, configuration.renderDiagnosticInternalName)
        reportIncompleteHierarchies()
        reportAlternativeSignatureErrors()
    }

    private class MyDiagnostic<E : PsiElement>(
        psiElement: E, factory: DiagnosticFactory0<E>, val message: String
    ) : SimpleDiagnostic<E>(psiElement, factory, Severity.ERROR) {

        override val isValid: Boolean = true
    }

    companion object {
        private val SYNTAX_ERROR_FACTORY = DiagnosticFactory0.create<PsiErrorElement>(Severity.ERROR)

        private fun reportDiagnostic(diagnostic: Diagnostic, reporter: DiagnosticMessageReporter, renderDiagnosticName: Boolean): Boolean {
            if (!diagnostic.isValid) return false

            val message = (diagnostic as? MyDiagnostic<*>)?.message ?: DefaultErrorMessages.render(diagnostic)
            val diagnosticFactoryName = diagnostic.factoryNameOrNull()
            val textToRender = when (renderDiagnosticName) {
                true -> diagnosticFactoryName?.let { "[$it] $message" } ?: message
                false -> message
            }

            reporter.report(
                diagnostic,
                diagnostic.psiFile,
                textToRender
            )

            return diagnostic.severity == Severity.ERROR
        }

        fun reportDiagnostics(
            unsortedDiagnostics: GenericDiagnostics<*>,
            reporter: DiagnosticMessageReporter,
            renderDiagnosticName: Boolean
        ): Boolean {
            var hasErrors = false
            val diagnostics = sortedDiagnostics(unsortedDiagnostics.all().filterIsInstance<Diagnostic>())
            for (diagnostic in diagnostics) {
                hasErrors = hasErrors or reportDiagnostic(diagnostic, reporter, renderDiagnosticName)
            }
            return hasErrors
        }

        fun reportDiagnostics(
            diagnostics: GenericDiagnostics<*>,
            messageCollector: MessageCollector,
            renderInternalDiagnosticName: Boolean
        ): Boolean {
            return reportDiagnostics(diagnostics, DefaultDiagnosticReporter(messageCollector), renderInternalDiagnosticName).also {
                SyntaxErrorReporter.reportSpecialErrors(
                    diagnostics.any { it.factory == Errors.INCOMPATIBLE_CLASS },
                    diagnostics.any { it.factory == Errors.PRE_RELEASE_CLASS },
                    diagnostics.any { it.factory == Errors.IR_WITH_UNSTABLE_ABI_COMPILED_CLASS },
                    messageCollector
                )
            }
        }

        // Reports K1 diagnostics ([org.jetbrains.kotlin.diagnostics.SimpleDiagnostic])
        fun reportSyntaxErrors(file: PsiElement, reporter: DiagnosticMessageReporter): SyntaxErrorReport {
            return SyntaxErrorReporter.reportSyntaxErrors(file) { element, message ->
                val diagnostic = MyDiagnostic(element, SYNTAX_ERROR_FACTORY, message)
                reportDiagnostic(diagnostic, reporter, renderDiagnosticName = false)
            }
        }

        fun reportSyntaxErrors(file: PsiElement, messageCollector: MessageCollector): SyntaxErrorReport {
            return reportSyntaxErrors(file, DefaultDiagnosticReporter(messageCollector))
        }
    }
}
