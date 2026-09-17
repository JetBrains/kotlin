// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt
error class Foo
interface I

class Bar : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>
interface Baz : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>
object Qux : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>
enum class Quux : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration */
