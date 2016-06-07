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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.Interner

import java.io.IOException
import java.io.OutputStream

import org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName

class StringTableImpl : StringTable {
    private class FqNameProto(val fqName: QualifiedName.Builder) {

        override fun hashCode(): Int {
            var result = 13
            result = 31 * result + fqName.parentQualifiedName
            result = 31 * result + fqName.shortName
            result = 31 * result + fqName.kind.hashCode()
            return result
        }

        override fun equals(obj: Any?): Boolean {
            if (obj == null || javaClass != obj.javaClass) return false

            val other = (obj as FqNameProto).fqName
            return fqName.parentQualifiedName == other.parentQualifiedName
                   && fqName.shortName == other.shortName
                   && fqName.kind == other.kind
        }
    }

    private val strings = Interner<String>()
    private val qualifiedNames = Interner<FqNameProto>()

    fun getSimpleNameIndex(name: Name): Int {
        return getStringIndex(name.asString())
    }

    override fun getStringIndex(string: String): Int {
        return strings.intern(string)
    }

    override fun getFqNameIndex(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        if (ErrorUtils.isError(descriptor)) {
            throw IllegalStateException("Cannot get FQ name of error class: " + descriptor)
        }

        val builder = QualifiedName.newBuilder()
        builder.kind = QualifiedName.Kind.CLASS

        val containingDeclaration = descriptor.containingDeclaration
        if (containingDeclaration is PackageFragmentDescriptor) {
            val packageFqName = containingDeclaration.fqName
            if (!packageFqName.isRoot) {
                builder.parentQualifiedName = getPackageFqNameIndex(packageFqName)
            }
        }
        else if (containingDeclaration is ClassDescriptor) {
            builder.parentQualifiedName = getFqNameIndex(containingDeclaration)
        }
        else {
            throw IllegalStateException("Cannot get FQ name of local class: " + descriptor)
        }

        builder.shortName = getStringIndex(descriptor.name.asString())

        return qualifiedNames.intern(FqNameProto(builder))
    }

    fun getPackageFqNameIndex(fqName: FqName): Int {
        var result = -1
        for (segment in fqName.pathSegments()) {
            val builder = QualifiedName.newBuilder()
            builder.shortName = getSimpleNameIndex(segment)
            if (result != -1) {
                builder.parentQualifiedName = result
            }
            result = qualifiedNames.intern(FqNameProto(builder))
        }
        return result
    }

    fun buildProto(): Pair<ProtoBuf.StringTable, ProtoBuf.QualifiedNameTable> {
        val strings = ProtoBuf.StringTable.newBuilder()
        for (simpleName in this.strings.allInternedObjects) {
            strings.addString(simpleName)
        }

        val qualifiedNames = ProtoBuf.QualifiedNameTable.newBuilder()
        for (fqName in this.qualifiedNames.allInternedObjects) {
            qualifiedNames.addQualifiedName(fqName.fqName)
        }

        return Pair(strings.build(), qualifiedNames.build())
    }

    override fun serializeTo(output: OutputStream) {
        try {
            val protos = buildProto()
            protos.first.writeDelimitedTo(output)
            protos.second.writeDelimitedTo(output)
        }
        catch (e: IOException) {
            throw rethrow(e)
        }

    }
}
