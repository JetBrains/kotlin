package org.jetbrains.kotlin.ir.util

// This is an abstract uniqIdIndex any serialized IR declarations gets.
// It is either isLocal and then just gets and ordinary number within its module.
// Or is visible across modules and then gets a hash of mangled name as its index.

inline class UniqId(val index: Long) {
    val isPublic: Boolean get() = (index and (1L shl 63)) != 0L
    val isLocal: Boolean get() = (index and (1L shl 63)) == 0L

    companion object {
        val NONE = UniqId(0x7fffffffL)
    }
}

