class <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>Foo1()<!> {}

public class Foo2() {
    <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun method()<!> {}
    public fun method2() {}
    private fun method3() {}

    <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE, NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun implicit()<!> = 10
    <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>public fun implicit2()<!> = 10
    public fun implicit3(): Int = 10
}

public data class FooData(val i: Int, val s: String)

data class <!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>FooData2(val i: Int, val s: String)<!>