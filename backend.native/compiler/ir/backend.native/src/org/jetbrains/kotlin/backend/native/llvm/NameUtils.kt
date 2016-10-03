package org.jetbrains.kotlin.backend.native.llvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.util.zip.CRC32

private fun crc32(str: String): Long {
    val c = CRC32()
    c.update(str.toByteArray())
    return c.value
}

internal val String.nameHash: Long
    get() = crc32(this)

internal val Name.nameHash: Long
    get() = this.toString().nameHash

internal val FqName.nameHash: Long
    get() = this.toString().nameHash

internal val FunctionDescriptor.symbolName: String
    get() = "kfun:" + this.fqNameSafe.toString() // FIXME: add signature

internal val ClassDescriptor.typeInfoSymbolName: String
    get() = "ktype:" + this.fqNameSafe.toString()