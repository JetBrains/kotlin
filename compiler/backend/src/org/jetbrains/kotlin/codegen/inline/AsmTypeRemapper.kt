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

class AsmTypeRemapper(val typeRemapper: TypeRemapper, val result: InlineResult) : Remapper() {
    override fun map(type: String): String {
        return typeRemapper.map(type)
    }

    override fun createRemappingSignatureAdapter(v: SignatureVisitor?): SignatureVisitor {
        return object : RemappingSignatureAdapter(v, this) {
            override fun visitTypeVariable(name: String) {
                /*TODO try to erase absent type variable*/
                val mapping = typeRemapper.mapTypeParameter(name) ?: return super.visitTypeVariable(name)

                if (mapping.newName != null) {
                    if (mapping.isReified) {
                        result.reifiedTypeParametersUsages.addUsedReifiedParameter(mapping.newName)
                    }
                    return super.visitTypeVariable(mapping.newName)
                }
                // else TypeVariable is replaced by concrete type
                SignatureReader(mapping.signature).accept(v)
            }

            override fun visitFormalTypeParameter(name: String) {
                typeRemapper.registerTypeParameter(name)
                super.visitFormalTypeParameter(name)
            }
        }
    }
}
