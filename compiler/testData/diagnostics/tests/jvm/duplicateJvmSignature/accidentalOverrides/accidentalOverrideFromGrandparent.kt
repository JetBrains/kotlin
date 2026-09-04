// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER
interface Foo<X> {
    fun foo(x: X)
}

open class FooImpl : Foo<String> {
    override fun foo(x: String) {
    }
}

open class FooImpl2 : FooImpl() {
    <!ACCIDENTAL_OVERRIDE!>fun foo(x: Any)<!> {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, nullableType, override,
typeParameter */
