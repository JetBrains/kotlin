// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM

data class Pair<A, B>(val first: A, val second: B)

context(Comparator<T>)
infix operator fun <T> T.compareTo(other: T) = compare(this, other)

context(Comparator<T>)
val <T> Pair<T, T>.min get() = if (first < second) first else second