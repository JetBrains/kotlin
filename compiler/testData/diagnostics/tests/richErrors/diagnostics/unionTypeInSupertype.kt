// RUN_PIPELINE_TILL: FRONTEND
error class Foo
interface I

class Bar : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>
interface Baz : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>
object Qux : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>
enum class Quux : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>I | Foo<!>

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration */
