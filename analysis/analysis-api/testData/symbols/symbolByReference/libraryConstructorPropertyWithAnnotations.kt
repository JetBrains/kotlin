// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

class SimpleClass(
    @Anno
    @get:Anno
    val constructorPropertyWithAnnotations: Short,
)

annotation class Anno

// MODULE: main(lib)
// FILE: usage.kt
fun usage(instance: one.SimpleClass) {
    instance.const<caret>ructorPropertyWithAnnotations
}