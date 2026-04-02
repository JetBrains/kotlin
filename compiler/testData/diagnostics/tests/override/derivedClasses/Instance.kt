// RUN_PIPELINE_TILL: BACKEND
interface Foo<T> {
    fun foo()
}

interface Bar : Foo<String>

fun usage(bar: Bar) {
    <!DEBUG_INFO_CALLABLE_OWNER("Bar.foo in implicit Bar")!>bar.foo()<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
