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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.org.objectweb.asm.commons.Remapper
import org.jetbrains.org.objectweb.asm.commons.RemappingSignatureAdapter
import org.jetbrains.org.objectweb.asm.signature.SignatureReader
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor

class AsmTypeRemapper(val typeRemapper: TypeRemapper, val mappings: TypeParameterMappings?, val result: InlineResult) : Remapper() {

    override fun map(type: String): String {
        return typeRemapper.map(type)
    }


    override fun createRemappingSignatureAdapter(v: SignatureVisitor?): SignatureVisitor {
        if (mappings == null) {
            //don't remap default generation
            return super.createRemappingSignatureAdapter(v);
        }

        return object : RemappingSignatureAdapter(v, this) {

            override fun visitTypeVariable(name: String) {
                val mapping = getMappingByName(name) ?:
                              return super.visitTypeVariable(name)

                if (mapping.newName != null) {
                    if (mapping.isReified) {
                        result.reifiedTypeParametersUsages.addUsedReifiedParameter(mapping.newName)
                    }
                    return super.visitTypeVariable(mapping.newName)
                }
                // else TypeVariable is replaced by concrete type
                SignatureReader(mapping.signature).accept(this)
            }

            override fun visitFormalTypeParameter(name: String) {
                val mapping: TypeParameterMapping = getMappingByName(name) ?:
                                   return super.visitFormalTypeParameter(name)
                if (mapping.newName != null ) {
                    if (mapping.isReified) {
                        result.reifiedTypeParametersUsages.addUsedReifiedParameter(mapping.newName)
                    }
                    super.visitFormalTypeParameter(mapping.newName)
                }
            }

            private fun getMappingByName(name: String): TypeParameterMapping? = mappings[name]
        }
    }


}