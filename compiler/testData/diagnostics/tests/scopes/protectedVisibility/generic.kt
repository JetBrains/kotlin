// RUN_PIPELINE_TILL: BACKEND
abstract class Parent<F> {
    protected fun foo() {}
}

class Derived<E> : Parent<E>() {
    fun bar(x: Derived<String>) {
        x.foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeParameter */
