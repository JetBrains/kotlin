/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.QualifiedNameTable.QualifiedName
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.getClassId

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
    when (proto.kind!!) {
        QualifiedName.Kind.CLASS,
        QualifiedName.Kind.LOCAL ->
            return module.findClassAcrossModuleDependencies(this.getClassId(fqNameIndex))!!
        QualifiedName.Kind.PACKAGE ->
            return module.getPackage(FqName(packageName))
    }
}

