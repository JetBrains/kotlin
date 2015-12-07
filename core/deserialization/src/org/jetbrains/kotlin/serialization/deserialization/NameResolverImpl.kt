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
import org.jetbrains.kotlin.utils.rethrow
import java.io.IOException
import java.io.InputStream
import java.util.*

class NameResolverImpl private constructor(
        private val strings: ProtoBuf.StringTable,
        private val qualifiedNames: ProtoBuf.QualifiedNameTable) : NameResolver {

    override fun getString(index: Int): String {
        return strings.getString(index)
    }

    override fun getName(index: Int): Name {
        return Name.guess(strings.getString(index))
    }

    override fun getClassId(index: Int): ClassId {
        var index = index
        val packageFqName = LinkedList<String>()
        val relativeClassName = LinkedList<String>()
        var local = false

        while (index != -1) {
            val proto = qualifiedNames.getQualifiedName(index)
            val shortName = strings.getString(proto.shortName)
            when (proto.kind!!) {
                QualifiedName.Kind.CLASS -> relativeClassName.addFirst(shortName)
                QualifiedName.Kind.PACKAGE -> packageFqName.addFirst(shortName)
                QualifiedName.Kind.LOCAL -> {
                    relativeClassName.addFirst(shortName)
                    local = true
                }
            }

            index = proto.parentQualifiedName
        }

        return ClassId(FqName.fromSegments(packageFqName), FqName.fromSegments(relativeClassName), local)
    }

    companion object {
        fun read(stream: InputStream): NameResolverImpl {
            try {
                val simpleNames = ProtoBuf.StringTable.parseDelimitedFrom(stream)
                val qualifiedNames = ProtoBuf.QualifiedNameTable.parseDelimitedFrom(stream)
                return NameResolverImpl(simpleNames, qualifiedNames)
            }
            catch (e: IOException) {
                throw rethrow(e)
            }

        }
    }
}
