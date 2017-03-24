// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

open class A
class B : A()
protocol interface ProtoInterface {
    fun foo(a: A, b: String): String
}

fun callFoo(arg: ProtoInterface) {
    val result: String = arg.foo(A(), "42")
}

fun test() {
    callFoo(<!TYPE_MISMATCH!>object<!> {
        fun foo(a: A): String = ""
    })

    callFoo(<!TYPE_MISMATCH!>object<!> {
        fun foo(a: A, b: String, c: Int): Int = 42
    })

    callFoo(<!TYPE_MISMATCH!>object<!> {
        fun foo(a: A, b: String): Int = 42
    })

    callFoo(object {
        fun foo(a: A, b: String): String = "42"
    })

    callFoo(<!TYPE_MISMATCH!>object<!> {
        fun foo(a: B, b: String): String = "42"
    })
}