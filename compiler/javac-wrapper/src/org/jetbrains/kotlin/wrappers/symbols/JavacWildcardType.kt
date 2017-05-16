/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.wrappers.symbols

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType

class JavacWildcardType<out T : TypeMirror>(typeMirror: T,
                                            javac: JavacWrapper) : JavacType<T>(typeMirror, javac), JavaWildcardType {

    override val bound
        get() = typeMirror.let {
            val boundMirror = (it as WildcardType).extendsBound ?: it.superBound
            boundMirror?.let { create(it, javac) }
        }

    override val isExtends
        get() = (typeMirror as WildcardType).extendsBound != null

}
