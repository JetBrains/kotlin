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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.ProtoBuf.QualifiedNameTable.QualifiedName
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl

// TODO Come up with a better file name.

internal fun NameResolverImpl.getDescriptorByFqNameIndex(
    module: ModuleDescriptor, 
    nameTable: ProtoBuf.QualifiedNameTable, 
    fqNameIndex: Int): DeclarationDescriptor {

    if (fqNameIndex == -1) return module.getPackage(FqName.ROOT)
    val packageName = this.getPackageFqName(fqNameIndex)
    // TODO: Here we are using internals of NameresolverImpl. 
    // Consider extending NameResolver.
    val proto = nameTable.getQualifiedName(fqNameIndex)
    when (proto.kind) {
        QualifiedName.Kind.CLASS,
        QualifiedName.Kind.LOCAL ->
            return module.findClassAcrossModuleDependencies(this.getClassId(fqNameIndex))!!
        QualifiedName.Kind.PACKAGE ->
            return module.getPackage(packageName)
    }
}

