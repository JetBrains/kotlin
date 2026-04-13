// RUN_PIPELINE_TILL: FRONTEND
object Scope1 {
    val someVar: Any = Any()

    fun foo() {
        <!UNRESOLVED_REFERENCE!>someVar<!>(1)
    }
}

object Scope2 {
    class Foo

    fun use() {
        val foo = Foo()
        <!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, nestedClass,
objectDeclaration, propertyDeclaration */
