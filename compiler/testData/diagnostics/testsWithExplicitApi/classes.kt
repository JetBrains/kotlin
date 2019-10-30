// SKIP_TXT

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>class Foo1<!>() {}

public class Foo2() {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun method<!>() {}
    public fun method2() {}
    private fun method3() {}

    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>implicit<!><!>() = 10
    public fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>implicit2<!>() = 10
    public fun implicit3(): Int = 10
}

public data class FooData(val i: Int, val s: String)

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>data class FooData2<!>(val i: Int, val s: String)

public class WithNested {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>class Nested<!> {}
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>inner class Inner<!> {}
}