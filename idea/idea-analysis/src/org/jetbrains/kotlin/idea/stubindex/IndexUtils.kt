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
import org.jetbrains.kotlin.util.aliasImportMap

fun <TDeclaration : KtCallableDeclaration> indexTopLevelExtension(stub: KotlinCallableStubBase<TDeclaration>, sink: IndexSink) {
    if (stub.isExtension()) {
        val declaration = stub.psi
        val containingTypeReference = declaration.receiverTypeReference!!
        containingTypeReference.typeElement?.index(declaration, sink, containingTypeReference)
    }
}

private fun <TDeclaration : KtCallableDeclaration> KtTypeElement.index(
        declaration: TDeclaration, sink: IndexSink, containingTypeReference: KtTypeReference
) {
    fun occurrence(typeName: String) {
        val name = declaration.name ?: return
        sink.occurrence(KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE.key,
                        KotlinTopLevelExtensionsByReceiverTypeIndex.buildKey(typeName, name))
    }

    when (this) {
        is KtUserType -> {
            var referenceName = referencedName ?: return

            val typeParameter = declaration.typeParameters.firstOrNull { it.name == referenceName }
            if (typeParameter != null) {
                val bound = typeParameter.extendsBound
                if (bound != null) {
                    bound.typeElement?.index(declaration, sink, containingTypeReference)
                }
                else {
                    occurrence("Any")
                }
                return
            }

            occurrence(referenceName)

            aliasImportMap()[referenceName].forEach { occurrence(it) }
        }

        is KtNullableType -> innerType?.index(declaration, sink, containingTypeReference)

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

        else -> error("Unsupported type: $this")
    }
}
