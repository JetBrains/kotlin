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

package org.jetbrains.kotlin.codegen.serialization

import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmNameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.StringTable
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.StringTableTypes.Record
import org.jetbrains.kotlin.types.ErrorUtils
import java.io.OutputStream
import java.util.*

class JvmStringTable(private val typeMapper: JetTypeMapper) : StringTable {
    public val strings = ArrayList<String>()
    private val records = ArrayList<Record>()
    private val map = HashMap<String, Int>()
    private val localNames = HashSet<Int>()

    override fun getStringIndex(string: String): Int =
            map.getOrPut(string) {
                strings.size().apply {
                    strings.add(string)
                    records.add(Record.newBuilder().apply {
                        // TODO: optimize, don't always store the operation
                        setOperation(Record.Operation.NONE)
                    }.build())
                }
            }

    override fun getFqNameIndex(descriptor: ClassDescriptor): Int {
        if (ErrorUtils.isError(descriptor)) {
            throw IllegalStateException("Cannot get FQ name of error class: " + descriptor)
        }

        // We use the following format to encode ClassId: "pkg/Outer.Inner".
        // It represents a unique name, but such names don't usually appear in the constant pool, so we're writing "Lpkg/Outer$Inner;"
        // instead and an instruction to drop the first and the last character in this string and replace all '$' with '.'.
        // This works most of the time, except in two rare cases:
        // - the name of the class or any of its outer classes contains dollars. In this case we're just storing the described
        //   string literally: "pkg/Outer.Inner$with$dollars"
        // - the class is local or nested in local. In this case we're also storing the literal string, and also storing the fact that
        //   this name represents a local class in a separate list

        val classId = descriptor.classId
        val string = classId.asString()

        map[string]?.let { recordedIndex ->
            // If we already recorded such string, we only return its index if it's local and our name is local
            // OR it's not local and our name is not local as well
            if (classId.isLocal == (recordedIndex in localNames)) {
                return recordedIndex
            }
        }

        val index = strings.size()
        if (classId.isLocal) {
            localNames.add(index)
        }

        val record = Record.newBuilder()

        // If the class is local or any of its outer class names contains '$', store a literal string
        if (classId.isLocal || '$' in string) {
            strings.add(string)
            // TODO: optimize, don't always store the operation
            record.setOperation(Record.Operation.NONE)
        }
        else {
            val predefinedIndex = JvmNameResolver.getPredefinedStringIndex(string)
            if (predefinedIndex != null) {
                record.setPredefinedIndex(predefinedIndex)
                record.setOperation(Record.Operation.NONE)
                // TODO: move all records with predefined names to the end and do not write associated strings for them (since they are ignored)
                strings.add("")
            }
            else {
                record.setOperation(Record.Operation.DESC_TO_CLASS_ID)
                strings.add("L${string.replace('.', '$')};")
            }
        }

        records.add(record.build())

        map[string] = index

        return index
    }

    private val ClassDescriptor.classId: ClassId
        get() {
            val container = containingDeclaration
            return when (container) {
                is ClassDescriptor -> container.classId.createNestedClassId(name)
                is PackageFragmentDescriptor -> ClassId(container.fqName, name)
                else -> {
                    val fqName = FqName(typeMapper.mapClass(this).internalName.replace('/', '.'))
                    ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), /* isLocal = */ true)
                }
            }
        }

    override fun serializeTo(output: OutputStream) {
        with(JvmProtoBuf.StringTableTypes.newBuilder()) {
            addAllRecord(records)
            addAllLocalName(localNames)
            build().writeDelimitedTo(output)
        }
    }
}
