// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Foo
interface Bar

interface A {
    fun <T> foo()
    where T : Foo, T : Bar
    = Unit
}

class B : A {
    override fun <T> foo()
    where T : Foo, T : Bar
    = Unit

}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, override, typeConstraint,
typeParameter */
