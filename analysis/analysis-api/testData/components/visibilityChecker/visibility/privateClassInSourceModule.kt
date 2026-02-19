// MODULE: dependency
// MODULE_KIND: Source
// FILE: Declaration.kt
private class Declaration

// MODULE: main(dependency)
// FILE: main.kt
fun foo() {
    p<caret>rintln()
}

// class: Declaration
