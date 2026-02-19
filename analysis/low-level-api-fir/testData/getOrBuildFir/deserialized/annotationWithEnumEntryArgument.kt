// KT-63330
// SKIP_WHEN_OUT_OF_CONTENT_ROOT

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: EnumClass.kt
enum class EnumClass {
    First, Second
}

@Target(AnnotationTarget.TYPE)
annotation class Anno(val enumEntry: EnumClass)

// MODULE: library2(library1)
// MODULE_KIND: LibraryBinary
// FILE: Usage.kt
class Usage

val usage: @Anno(EnumClass.First) Usage = Usage()

// MODULE: main(library1, library2)
// FILE: main.kt
fun foo() {
    <expr>val x = usage</expr>
}
