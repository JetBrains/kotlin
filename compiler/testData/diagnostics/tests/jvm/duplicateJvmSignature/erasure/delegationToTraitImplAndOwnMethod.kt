// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo<T> {
    fun foo(l: List<T>) {

    }
}

class <!CONFLICTING_JVM_DECLARATIONS!>Bar(f: Foo<String>)<!>: Foo<String> by f {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(l: List<Int>)<!> {}
}

class BarOther(f: Foo<String>): Foo<String> by f {
    override fun foo(l: List<String>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, nullableType,
override, primaryConstructor, typeParameter */
