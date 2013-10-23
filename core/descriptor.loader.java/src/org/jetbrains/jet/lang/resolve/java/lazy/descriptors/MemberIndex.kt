/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass

trait MemberIndex {
    fun findMethodsByName(name: Name): Collection<JavaMethod>
    fun getAllMetodNames(): Collection<Name>
}

object EMPTY_MEMBER_INDEX : MemberIndex {
    override fun findMethodsByName(name: Name) = listOf<JavaMethod>()
    override fun getAllMetodNames() = listOf<Name>()
}

class ClassMemberIndex(jClass: JavaClass, mustBeStatic: Boolean) : MemberIndex {
    private val methods = jClass.getMethods().iterator().filter { m -> m.isStatic() == mustBeStatic && !m.isConstructor() }.groupBy { m -> m.getName() }

    override fun findMethodsByName(name: Name): Collection<JavaMethod> {
        return methods[name] ?: listOf()
    }

    override fun getAllMetodNames(): Collection<Name> = methods.keySet()
}