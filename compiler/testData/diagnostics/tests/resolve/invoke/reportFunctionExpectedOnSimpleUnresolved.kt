// RUN_PIPELINE_TILL: FRONTEND
object Scope1 {
    val someVar: Any = Any()

    fun foo() {
        <!FUNCTION_EXPECTED!>someVar<!>(1)
    }
}

object Scope2 {
    class Foo

    fun use() {
        val foo = Foo()
        <!FUNCTION_EXPECTED!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, nestedClass,
objectDeclaration, propertyDeclaration */
