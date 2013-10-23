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
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.resolve.java.structure.JavaField
import org.jetbrains.jet.lang.resolve.java.structure.JavaMember
import org.jetbrains.jet.utils.valuesToMap

trait MemberIndex {
    fun findMethodsByName(name: Name): Collection<JavaMethod>
    fun getAllMetodNames(): Collection<Name>

    fun findFieldByName(name: Name): JavaField?
    fun getAllFieldNames(): Collection<Name>
}

object EMPTY_MEMBER_INDEX : MemberIndex {
    override fun findMethodsByName(name: Name) = listOf<JavaMethod>()
    override fun getAllMetodNames() = listOf<Name>()

    override fun findFieldByName(name: Name): JavaField? = null
    override fun getAllFieldNames() = listOf<Name>()
}

class ClassMemberIndex(val jClass: JavaClass, mustBeStatic: Boolean) : MemberIndex {
    private val memberFilter = {
        (m: JavaMember) ->
        m.isStatic() == mustBeStatic &&
        m.getVisibility() != Visibilities.PRIVATE
    }

    private val methodFilter = {
        (m: JavaMethod) ->
        memberFilter(m) &&
        !m.isConstructor() &&
        !DescriptorResolverUtils.isObjectMethodInInterface(m)
    }

    private val methods = jClass.getMethods().iterator().filter(methodFilter).groupBy { m -> m.getName() }
    private val fields = jClass.getFields().iterator().filter(memberFilter).valuesToMap { m -> m.getName() }

    override fun findMethodsByName(name: Name): Collection<JavaMethod> {
        return methods[name] ?: listOf()
    }

    override fun getAllMetodNames(): Collection<Name> = jClass.getAllMethods().iterator().filter(methodFilter).map { m -> m.getName() }.toList()

    override fun findFieldByName(name: Name): JavaField? = fields[name]
    override fun getAllFieldNames() = jClass.getAllFields().iterator().filter(memberFilter).map { m -> m.getName() }.toList()
}