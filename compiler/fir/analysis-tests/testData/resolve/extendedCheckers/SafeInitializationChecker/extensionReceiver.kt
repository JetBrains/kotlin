// KT-12152
class Test(extFun: Test.() -> String) {
    val x = <!VALUE_CANNOT_BE_PROMOTED!>this<!>.extFun()
}

val kaboom = Test { x }.x

class Outer {
    fun Nested.foo(): String = x

    inner class Nested(outer: Outer) {
        val x: String = <!VALUE_CANNOT_BE_PROMOTED!>this<!>.foo()
    }
}

