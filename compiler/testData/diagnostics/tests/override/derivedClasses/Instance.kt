// RUN_PIPELINE_TILL: BACKEND
interface Foo<T> {
    fun foo()
}

interface Bar : Foo<String>

fun usage(bar: Bar) {
    bar.<!DEBUG_INFO_CALLABLE_OWNER("Bar.foo in Bar")!>foo()<!>
}