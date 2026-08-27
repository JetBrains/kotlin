// class: pack/FullValueClassFromJvmAbiBinary
// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM

// MODULE: library
// MODULE_KIND: LibraryBinary
// JVM_ABI_GEN
// FILE: library.kt
package pack

value class FullValueClassFromJvmAbiBinary<T> private constructor(
    private val element: T,
    private val name: String,
) {
    companion object {
        fun <T> of(element: T): FullValueClassFromJvmAbiBinary<T> =
            FullValueClassFromJvmAbiBinary(element, "value")
    }
}

// MODULE: main(library)
// FILE: main.kt
fun consume(value: FullValueClassFromJvmAbiBinary<String>) {}
