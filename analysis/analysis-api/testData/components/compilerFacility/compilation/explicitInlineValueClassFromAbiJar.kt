// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_ABI_GEN
// COMPILER_ARGUMENTS: -Xfull-value-classes
// FILE: lib.kt
package lib

inline value class Wrapper private constructor(private val value: Int) {
    val doubled: Int get() = value * 2

    companion object {
        fun of(x: Int) = Wrapper(x)
    }
}

// MODULE: main(lib)
// LANGUAGE: +FullValueClasses
// FILE: main.kt
import lib.*

fun doubled(wrapper: Wrapper) = wrapper.doubled

fun test(): Int = doubled(Wrapper.of(1))
