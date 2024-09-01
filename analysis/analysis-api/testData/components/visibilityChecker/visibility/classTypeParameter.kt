// MODULE: dependency
// MODULE_KIND: Source
// FILE: Declaration.kt
class Declaration<T>

// MODULE: main(dependency)
// FILE: main.kt
fun foo() {
    p<caret>rintln()
}

// type_parameter: T: class: Declaration
