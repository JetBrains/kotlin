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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.load.kotlin.JvmNameResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.serialization.SerializerExtension
import org.jetbrains.kotlin.serialization.StringTable
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.StringTableTypes.Record
import org.jetbrains.kotlin.types.ErrorUtils
import java.io.OutputStream
import java.util.*

class JvmStringTable(private val extension: SerializerExtension) : StringTable {
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

        val string: String
        val storeAsLiteral: Boolean

        val parent = sequence(descriptor, DeclarationDescriptor::getContainingDeclaration).first { it !is ClassDescriptor }
        if (parent is PackageFragmentDescriptor) {
            val classId = descriptor.classId
            val packageName = classId.packageFqName
            val className = classId.relativeClassName.asString()
            string =
                    if (packageName.isRoot) className
                    else packageName.asString().replace('.', '/') + "/" + className

            // If any of the class names contains '$', we can't simply replace all '$' with '.' upon deserialization.
            // This case is rather rare, so we're storing a literal string
            storeAsLiteral = '$' in string && classId.relativeClassName.pathSegments().any { '$' in it.asString() }
        }
        else {
            storeAsLiteral = true
            string = descriptor.localClassName()
        }

        val isLocal = descriptor.containingDeclaration !is ClassOrPackageFragmentDescriptor

        map[string]?.let { recordedIndex ->
            if (isLocal == (recordedIndex in localNames)) {
                return recordedIndex
            }
        }

        val index = strings.size()
        if (isLocal) {
            localNames.add(index)
        }

        val record = Record.newBuilder()

        if (storeAsLiteral) {
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
                strings.add("L$string;")
            }
        }

        records.add(record.build())

        map[string] = index

        return index
    }

    private fun ClassDescriptor.localClassName(): String {
        val container = containingDeclaration
        return when (container) {
            is ClassDescriptor -> container.localClassName() + "." + name.asString()
            else -> extension.getLocalClassName(this)
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
