// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

@file:OptIn(UnsafeCastFunction::class)

package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun <TDeclaration : KtCallableDeclaration> indexTopLevelExtension(stub: KotlinCallableStubBase<TDeclaration>, sink: IndexSink) {
    KotlinTopLevelExtensionsByReceiverTypeIndex.indexExtension(stub, sink)
}

fun <TDeclaration : KtCallableDeclaration> indexExtensionInObject(stub: KotlinCallableStubBase<TDeclaration>, sink: IndexSink) {
    KotlinExtensionsInObjectsByReceiverTypeIndex.indexExtension(stub, sink)
}

private fun <TDeclaration : KtCallableDeclaration> KotlinExtensionsByReceiverTypeStubIndexHelper.indexExtension(
    stub: KotlinCallableStubBase<TDeclaration>,
    sink: IndexSink
) {
    if (!stub.isExtension()) return

    val declaration = stub.psi
    val callableName = declaration.name ?: return
    val containingTypeReference = declaration.receiverTypeReference!!
    containingTypeReference.typeElement?.index(declaration, containingTypeReference) { typeName ->
        val key = KotlinExtensionsByReceiverTypeStubIndexHelper.Companion.Key(typeName, callableName)
        sink.occurrence(indexKey, key.key)
    }
}

fun indexTypeAliasExpansion(stub: KotlinTypeAliasStub, sink: IndexSink) {
    val declaration = stub.psi
    val typeReference = declaration.getTypeReference() ?: return
    val typeElement = typeReference.typeElement ?: return
    typeElement.index(declaration, typeReference) { typeName ->
        sink.occurrence(KotlinTypeAliasByExpansionShortNameIndex.indexKey, typeName)
    }
}

private fun KtTypeElement.index(
    declaration: KtTypeParameterListOwner,
    containingTypeReference: KtTypeReference,
    occurrence: (String) -> Unit
) {
    fun KtTypeElement.indexWithVisited(
        declaration: KtTypeParameterListOwner,
        containingTypeReference: KtTypeReference,
        visited: MutableSet<KtTypeElement>,
        occurrence: (String) -> Unit
    ) {
        if (this in visited) return

        visited.add(this)

        when (this) {
            is KtUserType -> {
                val referenceName = referencedName ?: return

                val typeParameter = declaration.typeParameters.firstOrNull { it.name == referenceName }
                if (typeParameter != null) {
                    val bound = typeParameter.extendsBound
                    if (bound != null) {
                        bound.typeElement?.indexWithVisited(declaration, containingTypeReference, visited, occurrence)
                    } else {
                        occurrence("Any")
                    }
                    return
                }

                occurrence(referenceName)

                KotlinPsiHeuristics.unwrapImportAlias(this, referenceName).forEach { occurrence(it) }
            }

            is KtNullableType -> innerType?.indexWithVisited(declaration, containingTypeReference, visited, occurrence)

            is KtFunctionType -> {
                val arity = parameters.size + (if (receiverTypeReference != null) 1 else 0)
                val suspendPrefix =
                    if (containingTypeReference.modifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true)
                        "Suspend"
                    else
                        ""
                occurrence("${suspendPrefix}Function$arity")
            }

            is KtDynamicType -> occurrence("Any")

            // Currently, the only possible intersection type is "T & Any". "Any" is allowed to be only on RHS
            is KtIntersectionType ->
                getLeftTypeRef()?.typeElement?.indexWithVisited(declaration, containingTypeReference, visited, occurrence)

            else -> error("Unsupported type: $this")
        }
    }

    indexWithVisited(declaration, containingTypeReference, mutableSetOf(), occurrence)
}

fun indexInternals(stub: KotlinCallableStubBase<*>, sink: IndexSink) {
    val name = stub.name ?: return

    val modifierListStub = stub.modifierList ?: return

    if (!modifierListStub.hasModifier(KtTokens.INTERNAL_KEYWORD)) return

    if (stub.isTopLevel()) return

    if (modifierListStub.hasModifier(KtTokens.OPEN_KEYWORD) || modifierListStub.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
        sink.occurrence(KotlinOverridableInternalMembersShortNameIndex.indexKey, name)
    }
}

private val STRING_TEMPLATE_EMPTY_ARRAY = emptyArray<KtStringTemplateExpression>()
private val STRING_TEMPLATE_TYPES = TokenSet.create(KtStubElementTypes.STRING_TEMPLATE)

private fun ValueArgument.stringTemplateExpression(): KtStringTemplateExpression? {
    if (this is StubBasedPsiElement<*>) {
        stub?.let {
            val constantExpressions = it.getChildrenByType(STRING_TEMPLATE_TYPES, STRING_TEMPLATE_EMPTY_ARRAY)
            return constantExpressions.firstOrNull()
        }
    }
    return getArgumentExpression() as? KtStringTemplateExpression
}

// TODO: it has to be dropped as soon as JvmFileClassUtil.getLiteralStringFromAnnotation fix 1.5.20 in compiler
private fun JvmFileClassUtil.stringFromAnnotation(annotation: KtAnnotationEntry): String? {
    val stringTemplateExpression = annotation.valueArguments.firstOrNull()?.stringTemplateExpression()
    return stringTemplateExpression?.entries?.singleOrNull()?.safeAs<KtLiteralStringTemplateEntry>()?.text
}

fun indexJvmNameAnnotation(stub: KotlinAnnotationEntryStub, sink: IndexSink) {
    if (stub.getShortName() != JvmFileClassUtil.JVM_NAME_SHORT) return

    val jvmName = JvmFileClassUtil.stringFromAnnotation(stub.psi) ?: return
    val annotatedElementName = stub.parentStub.parentStub.annotatedJvmNameElementName ?: return

    if (annotatedElementName != jvmName) {
        sink.occurrence(KotlinJvmNameAnnotationIndex.indexKey, jvmName)
    }
}

private val StubElement<*>.annotatedJvmNameElementName: String?
    get() = when (this) {
        is KotlinFileStub -> psi.name
        is NamedStub -> name ?: ""
        is KotlinPropertyAccessorStub -> parentStub.safeAs<KotlinPropertyStub>()?.name ?: ""
        is KotlinPlaceHolderStub -> parentStub?.annotatedJvmNameElementName
        else -> null
    }

private val KotlinStubWithFqName<*>.modifierList: KotlinModifierListStub?
    get() = findChildStubByType(KtStubElementTypes.MODIFIER_LIST)

fun <TDeclaration : KtCallableDeclaration> KotlinCallableStubBase<TDeclaration>.isDeclaredInObject(): Boolean {
    if (isTopLevel()) return false
    val containingDeclaration = parentStub?.parentStub?.psi

    return containingDeclaration is KtObjectDeclaration && !containingDeclaration.isObjectLiteral()
}