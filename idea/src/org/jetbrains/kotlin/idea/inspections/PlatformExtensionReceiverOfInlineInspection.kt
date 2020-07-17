/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.EditorTextField
import org.intellij.lang.regexp.RegExpFileType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullabilityFlexible
import org.jetbrains.kotlin.types.isNullable
import java.awt.BorderLayout
import java.util.regex.PatternSyntaxException
import javax.swing.JPanel

class PlatformExtensionReceiverOfInlineInspection : AbstractKotlinInspection() {

    private var nameRegex: Regex? = defaultNamePattern.toRegex()
    var namePattern: String = defaultNamePattern
        set(value) {
            field = value
            nameRegex = try {
                value.toRegex()
            } catch (e: PatternSyntaxException) {
                null
            }
        }


    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                val languageVersionSettings = expression.languageVersionSettings
                if (!languageVersionSettings.supportsFeature(LanguageFeature.NullabilityAssertionOnExtensionReceiver)) {
                    return
                }
                val nameRegex = nameRegex
                val callExpression = expression.selectorExpression as? KtCallExpression ?: return
                val calleeText = callExpression.calleeExpression?.text ?: return
                if (nameRegex != null && !nameRegex.matches(calleeText)) {
                    return
                }

                val context = expression.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val extensionReceiverType = resolvedCall.extensionReceiver?.type ?: return
                if (!extensionReceiverType.isNullabilityFlexible()) return
                val descriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return
                if (!descriptor.isInline || descriptor.extensionReceiverParameter?.type?.isNullable() == true) return

                val receiverExpression = expression.receiverExpression
                val dataFlowValueFactory = receiverExpression.getResolutionFacade().getFrontendService(DataFlowValueFactory::class.java)
                val dataFlow = dataFlowValueFactory.createDataFlowValue(receiverExpression, extensionReceiverType, context, descriptor)
                val stableNullability = context.getDataFlowInfoBefore(receiverExpression).getStableNullability(dataFlow)
                if (!stableNullability.canBeNull()) return

                holder.registerProblem(
                    receiverExpression,
                    KotlinBundle.message("call.of.inline.function.with.nullable.extension.receiver.can.provoke.npe.in.kotlin.1.2"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    IntentionWrapper(AddExclExclCallFix(receiverExpression), receiverExpression.containingKtFile)
                )
            }
        }

    override fun createOptionsPanel() = OptionsPanel(this)

    class OptionsPanel internal constructor(owner: PlatformExtensionReceiverOfInlineInspection) : JPanel() {
        init {
            layout = BorderLayout()

            val regexField = EditorTextField(owner.namePattern, null, RegExpFileType.INSTANCE).apply {
                setOneLineMode(true)
            }
            regexField.document.addDocumentListener(object : DocumentListener {
                override fun documentChanged(e: DocumentEvent) {
                    owner.namePattern = regexField.text
                }
            })
            val labeledComponent = LabeledComponent.create(regexField, KotlinBundle.message("text.pattern"), BorderLayout.WEST)
            add(labeledComponent, BorderLayout.NORTH)
        }
    }

    companion object {
        const val defaultNamePattern = "(toBoolean)|(content.*)"
    }
}