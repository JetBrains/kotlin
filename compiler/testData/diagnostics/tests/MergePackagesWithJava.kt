// KT-689 Allow to put Java and Kotlin files in the same packages

// This is a stub test. One should not extend Java packages that come from libraries.
package java

val c : lang.Class<*>? = null

val <T> Array<T>?.length : Int get() = if (this != null) this.size else throw NullPointerException()
