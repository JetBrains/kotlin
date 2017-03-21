annotation class Foo(
        val a: IntArray = <!UNSUPPORTED_FEATURE!>[]<!>,
        val b: FloatArray = <!UNSUPPORTED_FEATURE!>[1f, 2f]<!>,
        val c: Array<String> = <!UNSUPPORTED_FEATURE!>["/"]<!>
)

@Foo
fun test1() {}

@Foo(a = <!UNSUPPORTED_FEATURE!>[1, 2]<!>, c = <!UNSUPPORTED_FEATURE!>["a"]<!>)
fun test2() {}

@Foo(<!UNSUPPORTED_FEATURE!>[1]<!>, <!UNSUPPORTED_FEATURE!>[3f]<!>, <!UNSUPPORTED_FEATURE!>["a"]<!>)
fun test3() {}

fun test4() {
    <!UNSUPPORTED!>[1, 2]<!>
}
