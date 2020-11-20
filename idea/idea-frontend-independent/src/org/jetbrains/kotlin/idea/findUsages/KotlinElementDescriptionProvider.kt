/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.psi.ElementDescriptionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import com.intellij.usageView.UsageViewLongNameLocation
import com.intellij.usageView.UsageViewShortNameLocation
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinBundleIndependent
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

open class KotlinElementDescriptionProviderBase : ElementDescriptionProvider {
    private tailrec fun KtNamedDeclaration.parentForFqName(): KtNamedDeclaration? {
        val parent = getStrictParentOfType<KtNamedDeclaration>() ?: return null
        if (parent is KtProperty && parent.isLocal) return parent.parentForFqName()
        return parent
    }

    private fun KtNamedDeclaration.name() = nameAsName ?: Name.special("<no name provided>")

    private fun KtNamedDeclaration.fqName(): FqNameUnsafe {
        containingClassOrObject?.let {
            if (it is KtObjectDeclaration && it.isCompanion()) {
                return it.fqName().child(name())
            }
            return FqNameUnsafe("${it.name()}.${name()}")
        }

        val internalSegments = generateSequence(this) { it.parentForFqName() }
            .filterIsInstance<KtNamedDeclaration>()
            .map { it.name ?: "<no name provided>" }
            .toList()
            .asReversed()
        val packageSegments = containingKtFile.packageFqName.pathSegments()
        return FqNameUnsafe((packageSegments + internalSegments).joinToString("."))
    }

    private fun KtTypeReference.renderShort(): String {
        return accept(
            object : KtVisitor<String, Unit>() {
                private val visitor get() = this

                override fun visitTypeReference(typeReference: KtTypeReference, data: Unit): String {
                    val typeText = typeReference.typeElement?.accept(this, data) ?: "???"
                    return if (typeReference.hasParentheses()) "($typeText)" else typeText
                }

                override fun visitDynamicType(type: KtDynamicType, data: Unit) = type.text

                override fun visitFunctionType(type: KtFunctionType, data: Unit): String {
                    return buildString {
                        type.receiverTypeReference?.let { append(it.accept(visitor, data)).append('.') }
                        type.parameters.joinTo(this, prefix = "(", postfix = ")") { it.accept(visitor, data) }
                        append(" -> ")
                        append(type.returnTypeReference?.accept(visitor, data) ?: "???")
                    }
                }

                override fun visitNullableType(nullableType: KtNullableType, data: Unit): String {
                    val innerTypeText = nullableType.innerType?.accept(this, data) ?: return "???"
                    return "$innerTypeText?"
                }

                override fun visitSelfType(type: KtSelfType, data: Unit) = type.text

                override fun visitUserType(type: KtUserType, data: Unit): String {
                    return buildString {
                        append(type.referencedName ?: "???")

                        val arguments = type.typeArguments
                        if (arguments.isNotEmpty()) {
                            arguments.joinTo(this, prefix = "<", postfix = ">") {
                                it.typeReference?.accept(visitor, data) ?: it.text
                            }
                        }
                    }
                }

                override fun visitParameter(parameter: KtParameter, data: Unit) = parameter.typeReference?.accept(this, data) ?: "???"
            },
            Unit
        )
    }

    //TODO Copied from stringUtil.kt
    private fun String.collapseSpaces(): String {
        val builder = StringBuilder()
        var haveSpaces = false
        for (c in this) {
            if (c.isWhitespace()) {
                haveSpaces = true
            } else {
                if (haveSpaces) {
                    builder.append(" ")
                    haveSpaces = false
                }
                builder.append(c)
            }
        }
        return builder.toString()
    }

    //TODO: Implement in FIR
    protected open val PsiElement.isRenameJavaSyntheticPropertyHandler get() = false
    protected open val PsiElement.isRenameKotlinPropertyProcessor get() = false

    override fun getElementDescription(element: PsiElement, location: ElementDescriptionLocation): String? {
        val shouldUnwrap = location !is UsageViewShortNameLocation && location !is UsageViewLongNameLocation
        val targetElement = if (shouldUnwrap) element.unwrapped ?: element else element

        fun elementKind() = when (targetElement) {
            is KtClass -> if (targetElement.isInterface())
                KotlinBundleIndependent.message("find.usages.interface")
            else
                KotlinBundleIndependent.message("find.usages.class")
            is KtObjectDeclaration -> if (targetElement.isCompanion())
                KotlinBundleIndependent.message("find.usages.companion.object")
            else
                KotlinBundleIndependent.message("find.usages.object")
            is KtNamedFunction -> KotlinBundleIndependent.message("find.usages.function")
            is KtPropertyAccessor -> KotlinBundleIndependent.message(
                "find.usages.for.property",
                (if (targetElement.isGetter)
                    KotlinBundleIndependent.message("find.usages.getter")
                else
                    KotlinBundleIndependent.message("find.usages.setter"))
            ) + " "
            is KtFunctionLiteral -> KotlinBundleIndependent.message("find.usages.lambda")
            is KtPrimaryConstructor, is KtSecondaryConstructor -> KotlinBundleIndependent.message("find.usages.constructor")
            is KtProperty -> if (targetElement.isLocal)
                KotlinBundleIndependent.message("find.usages.variable")
            else
                KotlinBundleIndependent.message("find.usages.property")
            is KtTypeParameter -> KotlinBundleIndependent.message("find.usages.type.parameter")
            is KtParameter -> KotlinBundleIndependent.message("find.usages.parameter")
            is KtDestructuringDeclarationEntry -> KotlinBundleIndependent.message("find.usages.variable")
            is KtTypeAlias -> KotlinBundleIndependent.message("find.usages.type.alias")
            is KtLabeledExpression -> KotlinBundleIndependent.message("find.usages.label")
            is KtImportAlias -> KotlinBundleIndependent.message("find.usages.import.alias")
            is KtLightClassForFacade -> KotlinBundleIndependent.message("find.usages.facade.class")
            else -> {
                //TODO Implement in FIR
                when {
                    targetElement.isRenameJavaSyntheticPropertyHandler -> KotlinBundleIndependent.message("find.usages.property")
                    targetElement.isRenameKotlinPropertyProcessor -> KotlinBundleIndependent.message("find.usages.property.accessor")
                    else -> null
                }
            }
        }

        val namedElement = if (targetElement is KtPropertyAccessor) {
            targetElement.parent as? KtProperty
        } else targetElement as? PsiNamedElement

        @Suppress("FoldInitializerAndIfToElvis")
        if (namedElement == null) {
            return if (targetElement is KtElement) "'" + StringUtil.shortenTextWithEllipsis(
                targetElement.text.collapseSpaces(),
                53,
                0
            ) + "'" else null
        }

        if (namedElement.language != KotlinLanguage.INSTANCE) return null

        return when (location) {
            is UsageViewTypeLocation -> elementKind()
            is UsageViewShortNameLocation, is UsageViewLongNameLocation -> namedElement.name
            is RefactoringDescriptionLocation -> {
                val kind = elementKind() ?: return null
                if (namedElement !is KtNamedDeclaration) return null
                val renderFqName = location.includeParent() &&
                        namedElement !is KtTypeParameter &&
                        namedElement !is KtParameter &&
                        namedElement !is KtConstructor<*>
                val desc = when (namedElement) {
                    is KtFunction -> {
                        val baseText = buildString {
                            append(namedElement.name ?: "")
                            namedElement.valueParameters.joinTo(this, prefix = "(", postfix = ")") {
                                (if (it.isVarArg) "vararg " else "") + (it.typeReference?.renderShort() ?: "")
                            }
                            namedElement.receiverTypeReference?.let { append(" on ").append(it.renderShort()) }
                        }
                        val parentFqName = if (renderFqName) namedElement.fqName().parent() else null
                        if (parentFqName?.isRoot != false) baseText else "${parentFqName.asString()}.$baseText"
                    }
                    else -> (if (renderFqName) namedElement.fqName().asString() else namedElement.name) ?: ""
                }

                "$kind ${CommonRefactoringUtil.htmlEmphasize(desc)}"
            }
            is HighlightUsagesDescriptionLocation -> {
                val kind = elementKind() ?: return null
                if (namedElement !is KtNamedDeclaration) return null
                "$kind ${namedElement.name}"
            }
            else -> null
        }
    }
}
