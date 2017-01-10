package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
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

