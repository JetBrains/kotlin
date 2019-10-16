package stopInInlineCallInFieldInClassWithNonDefaultPrimary

class A {
    var value = 42

    var b: String = "".inlineApply {
        {
            //Breakpoint!
            foo(value)
        }()
    }

    constructor(): this("")
    constructor(a: String) {}
}

fun foo(a: Any) {}

fun main(args: Array<String>) {
    A()
}

inline fun <T> T.inlineApply(block: T.() -> kotlin.Unit): T { this.block(); return this }