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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.utils.valuesToMap
import java.util.HashSet

trait MemberIndex {
    fun findMethodsByName(name: Name): Collection<JavaMethod>
    fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name>

    fun findFieldByName(name: Name): JavaField?
    fun getAllFieldNames(): Collection<Name>
}

object EMPTY_MEMBER_INDEX : MemberIndex {
    override fun findMethodsByName(name: Name) = listOf<JavaMethod>()
    override fun getMethodNames(nameFilter: (Name) -> Boolean) = listOf<Name>()

    override fun findFieldByName(name: Name): JavaField? = null
    override fun getAllFieldNames() = listOf<Name>()
}

open class ClassMemberIndex(val jClass: JavaClass, val memberFilter: (JavaMember) -> Boolean) : MemberIndex {
    private val methodFilter = {
        (m: JavaMethod) ->
        memberFilter(m) && !DescriptorResolverUtils.isObjectMethodInInterface(m)
    }

    private val methods = jClass.getMethods().stream().filter(methodFilter).groupBy { m -> m.getName() }
    private val fields = jClass.getFields().stream().filter(memberFilter).valuesToMap { m -> m.getName() }

    override fun findMethodsByName(name: Name): Collection<JavaMethod> = methods[name] ?: listOf()
    override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name> = jClass.getAllMemberNames(methodFilter) { getMethods() }

    override fun findFieldByName(name: Name): JavaField? = fields[name]
    override fun getAllFieldNames(): Collection<Name> = jClass.getAllMemberNames(memberFilter) { getFields() }
}

private fun <M : JavaMember> JavaClass.getAllMemberNames(filter: (M) -> Boolean, getMembers: JavaClass.() -> Collection<M>): Set<Name> {
    val result = HashSet<Name>()
    val visitedSuperClasses = HashSet<JavaClass>()

    fun JavaClass.visit(): Unit {
        if (!visitedSuperClasses.add(this)) return

        for (member in getMembers()) {
            if (filter(member)) {
                result.add(member.getName())
            }
        }

        for (supertype in getSupertypes()) {
            val classifier = supertype.getClassifier()
            if (classifier is JavaClass) {
                classifier.visit()
            }
        }
    }

    this.visit()
    return result
}
