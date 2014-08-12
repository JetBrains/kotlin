/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor

// To facilitate laziness, any JetType implementation may inherit from this trait,
// even if it turns out that the type an instance represents is not actually a type variable
// (i.e. it is not derived from a type parameter), see isTypeVariable
public trait CustomTypeVariable : JetType {
    val isTypeVariable: Boolean

    // If typeParameterDescriptor != null <=> isTypeVariable == true, this is not a type variable
    val typeParameterDescriptor: TypeParameterDescriptor?


    // Throws an exception when isTypeVariable == false
    fun substitutionResult(replacement: JetType): JetType
}

fun JetType.isCustomTypeVariable() = (this as? CustomTypeVariable)?.isTypeVariable ?: false