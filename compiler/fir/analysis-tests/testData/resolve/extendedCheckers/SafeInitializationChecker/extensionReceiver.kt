// KT-12152
class Test(extFun: Test.() -> String) {
    val x = <!VALUE_CANNOT_BE_PROMOTED!>extFun()<!>
}

val kaboom = Test { x }.x

class Outer {
    fun Nested.foo(): String = x

    class Nested(outer: Outer) {
        val x: String = with(outer) { <!VALUE_CANNOT_BE_PROMOTED!>foo()<!> }
    }
}

