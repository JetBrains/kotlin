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

package org.jetbrains.kotlin.codegen.signature

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.load.kotlin.JvmTypeFactory
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

object AsmTypeFactory : JvmTypeFactory<Type> {
    override fun boxType(possiblyPrimitiveType: Type) = AsmUtil.boxType(possiblyPrimitiveType)
    override fun createFromString(representation: String) = Type.getType(representation)
    override fun createObjectType(internalName: String) = Type.getObjectType(internalName)
    override fun toString(type: Type) = type.descriptor

    override val javaLangClassType: Type
        get() = AsmTypes.JAVA_CLASS_TYPE
}
