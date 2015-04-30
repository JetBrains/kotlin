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

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.openapi.util.Key
import com.intellij.psi.stubs.IndexSink
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinCallableStubBase
import org.jetbrains.kotlin.util.aliasImportMap

fun indexTopLevelExtension<TDeclaration : JetCallableDeclaration>(stub: KotlinCallableStubBase<TDeclaration>, sink: IndexSink) {
    if (stub.isExtension()) {
        val declaration = stub.getPsi()
        declaration.getReceiverTypeReference()!!.getTypeElement()?.index(declaration, sink)
    }
}

private fun JetTypeElement.index<TDeclaration : JetCallableDeclaration>(declaration: TDeclaration, sink: IndexSink) {
    fun occurrence(typeName: String) {
        val name = declaration.getName() ?: return
        sink.occurrence(JetTopLevelExtensionsByReceiverTypeIndex.INSTANCE.getKey(),
                        JetTopLevelExtensionsByReceiverTypeIndex.buildKey(typeName, name))
    }

    when (this) {
        is JetUserType -> {
            var referenceName = getReferencedName() ?: return

            val typeParameter = declaration.getTypeParameters().firstOrNull { it.getName() == referenceName }
            if (typeParameter != null) {
                val bound = typeParameter.getExtendsBound()
                if (bound != null) {
                    bound.getTypeElement()?.index(declaration, sink)
                }
                else {
                    occurrence("Any")
                }
                return
            }

            occurrence(referenceName)

            aliasImportMap()[referenceName].forEach { occurrence(it) }
        }

        is JetNullableType -> getInnerType()?.index(declaration, sink)

        is JetFunctionType -> {
            val arity = getParameters().size() + (if (getReceiverTypeReference() != null) 1 else 0)
            occurrence("Function$arity")
        }

        is JetDynamicType -> occurrence("Any")

        else -> error("Unsupported type: $this")
    }
}

