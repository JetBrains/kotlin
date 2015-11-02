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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinCallableStubBase
import org.jetbrains.kotlin.util.aliasImportMap

fun indexTopLevelExtension<TDeclaration : KtCallableDeclaration>(stub: KotlinCallableStubBase<TDeclaration>, sink: IndexSink) {
    if (stub.isExtension()) {
        val declaration = stub.getPsi()
        declaration.getReceiverTypeReference()!!.typeElement?.index(declaration, sink)
    }
}

private fun KtTypeElement.index<TDeclaration : KtCallableDeclaration>(declaration: TDeclaration, sink: IndexSink) {
    fun occurrence(typeName: String) {
        val name = declaration.getName() ?: return
        sink.occurrence(KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE.getKey(),
                        KotlinTopLevelExtensionsByReceiverTypeIndex.buildKey(typeName, name))
    }

    when (this) {
        is KtUserType -> {
            var referenceName = getReferencedName() ?: return

            val typeParameter = declaration.getTypeParameters().firstOrNull { it.getName() == referenceName }
            if (typeParameter != null) {
                val bound = typeParameter.getExtendsBound()
                if (bound != null) {
                    bound.typeElement?.index(declaration, sink)
                }
                else {
                    occurrence("Any")
                }
                return
            }

            occurrence(referenceName)

            aliasImportMap()[referenceName].forEach { occurrence(it) }
        }

        is KtNullableType -> getInnerType()?.index(declaration, sink)

        is KtFunctionType -> {
            val arity = getParameters().size() + (if (getReceiverTypeReference() != null) 1 else 0)
            occurrence("Function$arity")
        }

        is KtDynamicType -> occurrence("Any")

        else -> error("Unsupported type: $this")
    }
}
