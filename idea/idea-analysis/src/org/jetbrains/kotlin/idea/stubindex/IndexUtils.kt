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

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.psi.stubs.IndexSink
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinCallableStubBase
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.util.aliasImportMap

fun <TDeclaration : KtCallableDeclaration> indexTopLevelExtension(stub: KotlinCallableStubBase<TDeclaration>, sink: IndexSink) {
    if (stub.isExtension()) {
        val declaration = stub.psi
        val containingTypeReference = declaration.receiverTypeReference!!
        index(containingTypeReference.typeElement, declaration, containingTypeReference) { typeName ->
            val name = declaration.name ?: return@index
            sink.occurrence(
                KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE.key,
                KotlinTopLevelExtensionsByReceiverTypeIndex.buildKey(typeName, name)
            )
        }
    }
}

fun indexTypeAliasExpansion(stub: KotlinTypeAliasStub, sink: IndexSink) {
    val declaration = stub.psi
    val typeReference = declaration.getTypeReference() ?: return
    val typeElement = typeReference.typeElement ?: return
    index(typeElement, declaration, typeReference) { typeName ->
        sink.occurrence(KotlinTypeAliasByExpansionShortNameIndex.KEY, typeName)
    }
}

private fun index(
    typeElement: KtTypeElement?,
    declaration: KtTypeParameterListOwner,
    containingTypeReference: KtTypeReference,
    occurrence: (String) -> Unit
) {
    fun internalIndex(
        typeElement: KtTypeElement?,
        declaration: KtTypeParameterListOwner,
        containingTypeReference: KtTypeReference,
        visited: MutableSet<KtTypeElement>,
        occurrence: (String) -> Unit
    ) {
        if (typeElement == null) return
        if (typeElement in visited) return

        visited.add(typeElement)

        when (typeElement) {
            is KtUserType -> {
                val referenceName = typeElement.referencedName ?: return

                val typeParameter = declaration.typeParameters.firstOrNull { it.name == referenceName }
                if (typeParameter != null) {
                    val bound = typeParameter.extendsBound
                    if (bound != null) {
                        internalIndex(bound.typeElement, declaration, containingTypeReference, visited, occurrence)
                    } else {
                        occurrence("Any")
                    }
                    return
                }

                occurrence(referenceName)

                typeElement.aliasImportMap()[referenceName].forEach { occurrence(it) }
            }

            is KtNullableType -> internalIndex(typeElement.innerType, declaration, containingTypeReference, visited, occurrence)

            is KtFunctionType -> {
                val arity = typeElement.parameters.size + (if (typeElement.receiverTypeReference != null) 1 else 0)
                val suspendPrefix =
                    if (containingTypeReference.modifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true)
                        "Suspend"
                    else
                        ""
                occurrence("${suspendPrefix}Function$arity")
            }

            is KtDynamicType -> occurrence("Any")

            else -> error("Unsupported type: $typeElement")
        }
    }

    internalIndex(typeElement, declaration, containingTypeReference, mutableSetOf(), occurrence)
}

fun indexInternals(stub: KotlinCallableStubBase<*>, sink: IndexSink) {
    val name = stub.name ?: return

    val modifierListStub = stub.modifierList ?: return

    if (!modifierListStub.hasModifier(KtTokens.INTERNAL_KEYWORD)) return

    if (stub.isTopLevel()) return

    if (modifierListStub.hasModifier(KtTokens.OPEN_KEYWORD)
        || modifierListStub.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
        sink.occurrence(KotlinOverridableInternalMembersShortNameIndex.Instance.key, name)
    }
}

private val KotlinStubWithFqName<*>.modifierList: KotlinModifierListStub?
    get() = findChildStubByType(KtStubElementTypes.MODIFIER_LIST) as? KotlinModifierListStub
