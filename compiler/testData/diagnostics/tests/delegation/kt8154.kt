// RUN_PIPELINE_TILL: BACKEND
interface A<T> {
    fun foo()
}

interface B<T> : A<T> {
    fun bar()
}

class BImpl<T>(a: A<T>) : B<T>, A<T> by a {
    override fun bar() { throw UnsupportedOperationException() }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, nullableType,
override, primaryConstructor, typeParameter */
