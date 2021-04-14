/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentOwner
import org.jetbrains.kotlin.idea.core.TemplateKind
import org.jetbrains.kotlin.idea.core.getFunctionBodyTextFromTemplate
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.RendererModifier
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtPossibleExtensionSymbol
import org.jetbrains.kotlin.idea.j2k.IdeaDocCommentConverter
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.renderer.render
import javax.swing.Icon

internal data class KtClassMemberInfo(
    // TODO: use a `KtSymbolPointer` instead to avoid storing `KtSymbol` in an object after KT-46249 is fixed.
    val symbol: KtCallableSymbol,
    val memberText: String,
    val memberIcon: Icon?,
    val containingSymbolText: String?,
    val containingSymbolIcon: Icon?,
) {
    val isProperty: Boolean get() = symbol is KtPropertySymbol
}

internal class KtClassMember(
    private val memberInfo: KtClassMemberInfo,
    val bodyType: BodyType,
    val preferConstructorParameter: Boolean
) : MemberChooserObjectBase(
    memberInfo.memberText,
    memberInfo.memberIcon,
), ClassMember {
    val symbol = memberInfo.symbol
    override fun getParentNodeDelegate(): MemberChooserObject? =
        memberInfo.containingSymbolText?.let {
            KtClassOrObjectSymbolChooserObject(
                memberInfo.containingSymbolText,
                memberInfo.containingSymbolIcon
            )
        }
}

private data class KtClassOrObjectSymbolChooserObject(
    val symbolText: String?,
    val symbolIcon: Icon?
) :
    MemberChooserObjectBase(symbolText, symbolIcon)

internal fun createKtClassMember(
    memberInfo: KtClassMemberInfo,
    bodyType: BodyType,
    preferConstructorParameter: Boolean
): KtClassMember {
    return KtClassMember(memberInfo, bodyType, preferConstructorParameter)
}

internal fun KtAnalysisSession.generateMember(
    project: Project,
    ktClassMember: KtClassMember,
    targetClass: KtClassOrObject?,
    copyDoc: Boolean,
    mode: MemberGenerateMode = MemberGenerateMode.OVERRIDE
): KtCallableDeclaration = with(ktClassMember) {
    val bodyType = when {
        targetClass?.hasExpectModifier() == true -> BodyType.NO_BODY
        (symbol as? KtPossibleExtensionSymbol)?.isExtension == true && mode == MemberGenerateMode.OVERRIDE -> BodyType.FROM_TEMPLATE
        else -> bodyType
    }

    val renderOptions = when (mode) {
        MemberGenerateMode.OVERRIDE -> RenderOptions.overrideRenderOptions
        MemberGenerateMode.ACTUAL -> RenderOptions.actualRenderOptions
        MemberGenerateMode.EXPECT -> RenderOptions.expectRenderOptions
    }
    if (preferConstructorParameter && symbol is KtPropertySymbol) {
        return generateConstructorParameter(project, symbol, renderOptions, mode == MemberGenerateMode.OVERRIDE)
    }


    val newMember: KtCallableDeclaration = when (symbol) {
        is KtFunctionSymbol -> generateFunction(project, symbol, renderOptions, bodyType, mode == MemberGenerateMode.OVERRIDE)
        is KtPropertySymbol -> generateProperty(project, symbol, renderOptions, bodyType, mode == MemberGenerateMode.OVERRIDE)
        else -> error("Unknown member to override: $symbol")
    }

    when (mode) {
        MemberGenerateMode.ACTUAL -> newMember.addModifier(KtTokens.ACTUAL_KEYWORD)
        MemberGenerateMode.EXPECT -> if (targetClass == null) {
            newMember.addModifier(KtTokens.EXPECT_KEYWORD)
        }
        MemberGenerateMode.OVERRIDE -> {
            // TODO: add `actual` keyword to the generated member if the target class has `actual` and the generated member corresponds to
            //  an `expect` member.
        }
    }

    if (copyDoc) {
        val kDoc = when (val originalOverriddenPsi = symbol.originalOverriddenSymbol?.psi) {
            is KtDeclaration ->
                findDocComment(originalOverriddenPsi)
            is PsiDocCommentOwner -> {
                val kDocText = originalOverriddenPsi.docComment?.let { IdeaDocCommentConverter.convertDocComment(it) }
                if (kDocText.isNullOrEmpty()) null else KDocElementFactory(project).createKDocFromText(kDocText)
            }
            else -> null
        }
        if (kDoc != null) {
            newMember.addAfter(kDoc, null)
        }
    }

    return newMember
}

private fun KtAnalysisSession.generateConstructorParameter(
    project: Project,
    symbol: KtCallableSymbol,
    renderOptions: KtDeclarationRendererOptions,
    isOverride: Boolean,
): KtCallableDeclaration {
    return KtPsiFactory(project).createParameter(symbol.render(renderOptions.copy(forceRenderingOverrideModifier = isOverride)))
}

private fun KtAnalysisSession.generateFunction(
    project: Project,
    symbol: KtFunctionSymbol,
    renderOptions: KtDeclarationRendererOptions,
    bodyType: BodyType,
    isOverride: Boolean,
): KtCallableDeclaration {
    val returnType = symbol.annotatedType.type
    val returnsUnit = returnType.isUnit

    val body = if (bodyType != BodyType.NO_BODY) {
        val delegation = generateUnsupportedOrSuperCall(project, symbol, bodyType, returnsUnit)
        val returnPrefix = if (!returnsUnit && bodyType.requiresReturn) "return " else ""
        "{$returnPrefix$delegation\n}"
    } else ""

    val factory = KtPsiFactory(project)
    val functionText = symbol.render(renderOptions.copy(forceRenderingOverrideModifier = isOverride)) + body
    return when (symbol) {
        is KtConstructorSymbol -> {
            if (symbol.isPrimary) {
                factory.createPrimaryConstructor(functionText)
            } else {
                factory.createSecondaryConstructor(functionText)
            }
        }
        else -> factory.createFunction(functionText)
    }
}

private fun KtAnalysisSession.generateProperty(
    project: Project,
    symbol: KtPropertySymbol,
    renderOptions: KtDeclarationRendererOptions,
    bodyType: BodyType,
    isOverride: Boolean,
): KtCallableDeclaration {
    val returnType = symbol.annotatedType.type
    val returnsNotUnit = !returnType.isUnit

    val body =
        if (bodyType != BodyType.NO_BODY) {
            buildString {
                append("\nget()")
                append(" = ")
                append(generateUnsupportedOrSuperCall(project, symbol, bodyType, !returnsNotUnit))
                if (!symbol.isVal) {
                    append("\nset(value) {}")
                }
            }
        } else ""
    return KtPsiFactory(project).createProperty(symbol.render(renderOptions.copy(forceRenderingOverrideModifier = isOverride)) + body)
}

private fun <T> KtAnalysisSession.generateUnsupportedOrSuperCall(
    project: Project,
    symbol: T,
    bodyType: BodyType,
    canBeEmpty: Boolean = true
): String where T : KtNamedSymbol, T : KtCallableSymbol {
    when (bodyType.effectiveBodyType(canBeEmpty)) {
        BodyType.EMPTY_OR_TEMPLATE -> return ""
        BodyType.FROM_TEMPLATE -> {
            val templateKind = when (symbol) {
                is KtFunctionSymbol -> TemplateKind.FUNCTION
                is KtPropertySymbol -> TemplateKind.PROPERTY_INITIALIZER
                else -> throw IllegalArgumentException("$symbol must be either a function or a property")
            }
            return getFunctionBodyTextFromTemplate(
                project,
                templateKind,
                symbol.name.asString(),
                symbol.annotatedType.type.render(),
                null
            )
        }
        else -> return buildString {
            if (bodyType is BodyType.Delegate) {
                append(bodyType.receiverName)
            } else {
                append("super")
                if (bodyType == BodyType.QUALIFIED_SUPER) {
                    val superClassFqName = symbol.originalContainingClassForOverride?.name?.render()
                    superClassFqName?.let {
                        append("<").append(superClassFqName).append(">")
                    }
                }
            }
            append(".").append(symbol.name.render())

            if (symbol is KtFunctionSymbol) {
                val paramTexts = symbol.valueParameters.map {
                    val renderedName = it.name.render()
                    if (it.isVararg) "*$renderedName" else renderedName
                }
                paramTexts.joinTo(this, prefix = "(", postfix = ")")
            }
        }
    }
}

private object RenderOptions {
    // TODO: Currently rendering has the following problems:
//  - flexible types are not rendered correctly, specifically there are problems with the following
//    - flexible null type is rendered with `!`, which is not valid Kotlin code
//    - Array<(out) ...> (example idea/testData/codeInsight/overrideImplement/javaParameters/foo/Impl.kt.fir.after)
//    - incorrect type parameter (example idea/testData/codeInsight/overrideImplement/jdk8/overrideCollectionStream.kt.fir.after)
//  - some type annotations should be filtered, for example
//    - javax.annotation.Nonnull
//    - androidx.annotation.RecentlyNonNull
//    - org.jetbrains.annotations.NotNull
    val overrideRenderOptions = KtDeclarationRendererOptions(
        modifiers = setOf(RendererModifier.OVERRIDE, RendererModifier.ANNOTATIONS),
        approximateTypes = true,
        renderDefaultParameterValue = false,
    )

    val actualRenderOptions = overrideRenderOptions.copy(
        modifiers = setOf(RendererModifier.VISIBILITY, RendererModifier.MODALITY, RendererModifier.OVERRIDE, RendererModifier.INNER)

    )
    val expectRenderOptions = actualRenderOptions.copy(
        modifiers = actualRenderOptions.modifiers + RendererModifier.ACTUAL
    )
}
