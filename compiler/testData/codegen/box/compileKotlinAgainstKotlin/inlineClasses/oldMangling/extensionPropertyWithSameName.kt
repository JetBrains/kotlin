// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
// FILE: A.kt

inline class A(val value: String) {
    val Char.value: String get() = this + nonExtensionValue()

    fun nonExtensionValue(): String = value
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String = with(A("K")) { 'O'.value }
