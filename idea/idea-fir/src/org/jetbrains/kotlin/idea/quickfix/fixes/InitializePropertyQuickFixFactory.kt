/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.fixes

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.api.applicator.applicator
import org.jetbrains.kotlin.idea.fir.api.fixes.HLQuickFix
import org.jetbrains.kotlin.idea.fir.api.fixes.diagnosticFixFactory
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

object InitializePropertyQuickFixFactory {

    data class AddInitializerInput(val initializerText: String?) : HLApplicatorInput

    private val addInitializerApplicator: HLApplicator<KtProperty, AddInitializerInput> = applicator {
        familyAndActionName(KotlinBundle.lazyMessage("add.initializer"))

        applyToWithEditorRequired { property, input, project, editor ->
            val initializer = property.setInitializer(KtPsiFactory(project).createExpression(input.initializerText ?: "TODO()"))!!
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
            editor.selectionModel.setSelection(initializer.startOffset, initializer.endOffset)
            editor.caretModel.moveToOffset(initializer.endOffset)
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    val initializePropertyFactory =
        diagnosticFixFactory<KtFirDiagnostic.MustBeInitializedOrBeAbstract> { diagnostic ->
            val property: KtProperty = diagnostic.psi
            buildList {
                add(
                    HLQuickFix(
                        property,
                        AddInitializerInput(property.getReturnKtType().defaultInitializer),
                        addInitializerApplicator
                    )
                )

                (property.containingClassOrObject as? KtClass)?.let { ktClass ->
                    if (ktClass.isAnnotation() || ktClass.isInterface()) return@let

                    // TODO: Add quickfixes MoveToConstructorParameters and InitializeWithConstructorParameter after change signature
                    //  refactoring is available. See org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory
                }
            }
        }
}
