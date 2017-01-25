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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName
import java.util.*

class NameResolverImpl(
        private val strings: ProtoBuf.StringTable,
        private val qualifiedNames: ProtoBuf.QualifiedNameTable
) : NameResolver {

    override fun getString(index: Int): String = strings.getString(index)

    override fun getName(index: Int) = Name.guessByFirstCharacter(strings.getString(index))

    override fun getClassId(index: Int): ClassId {
        val (packageFqNameSegments, relativeClassNameSegments, isLocal) = traverseIds(index)
        return ClassId(FqName.fromSegments(packageFqNameSegments), FqName.fromSegments(relativeClassNameSegments), isLocal)
    }

    fun getPackageFqName(index: Int): FqName {
        val packageNameSegments = traverseIds(index).first
        return FqName.fromSegments(packageNameSegments)
    }

    private fun traverseIds(startingIndex: Int): Triple<List<String>, List<String>, Boolean> {
        var index = startingIndex
        val packageNameSegments = LinkedList<String>()
        val relativeClassNameSegments = LinkedList<String>()
        var local = false

        while (index != -1) {
            val proto = qualifiedNames.getQualifiedName(index)
            val shortName = strings.getString(proto.shortName)
            when (proto.kind!!) {
                QualifiedName.Kind.CLASS -> relativeClassNameSegments.addFirst(shortName)
                QualifiedName.Kind.PACKAGE -> packageNameSegments.addFirst(shortName)
                QualifiedName.Kind.LOCAL -> {
                    relativeClassNameSegments.addFirst(shortName)
                    local = true
                }
            }

            index = proto.parentQualifiedName
        }
        return Triple(packageNameSegments, relativeClassNameSegments, local)
    }
}
