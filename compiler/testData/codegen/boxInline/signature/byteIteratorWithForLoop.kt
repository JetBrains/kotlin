// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR JVM_IR_SERIALIZE
// ALLOW_KOTLIN_PACKAGE
// Note: order of files is important

// MODULE: kotlin_stdlib
// FILE: _Arrays.kt
package kotlin.collections

public inline fun ByteArray.customFirst(predicate: (Byte) -> Boolean): Byte {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Array contains no element matching the predicate.")
}

// FILE: PrimitiveIterators.kt
package kotlin.collections

public abstract class ByteIterator : Iterator<Byte> {
    override final fun next() = nextByte()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextByte(): Byte
}

// Module: main(kotlin_stdlib)
// FILE: main.kt
fun box(): String {
    byteArrayOf(1, 2, 3).customFirst { it == 1.toByte() }
    return "OK"
}
