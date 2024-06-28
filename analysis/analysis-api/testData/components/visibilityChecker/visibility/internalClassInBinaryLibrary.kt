// MODULE: dependency
// MODULE_KIND: LibraryBinary
// FILE: Declaration.kt
internal class Declaration

// MODULE: main(dependency)
// FILE: main.kt
fun foo() {
    p<caret>rintln()
}

// class: Declaration
