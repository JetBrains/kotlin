package onClassHeader

fun main(args: Array<String>) {
    val d: Base<String> = Derived()
    //Breakpoint!
    d.foo("")
    val a = 1
}

open class Base<T> {
    open fun foo(t: T): Int {
        return 1
    }
}

class Derived: Base<String>() {
    override fun foo(t: String): Int {
        return 2
    }
}

// STEP_INTO: 1

// EXPRESSION: 1 + 1
// RESULT: 2: I

// EXPRESSION: this
// RESULT: instance of onClassHeader.Derived(id=ID): LonClassHeader/Derived;

// EXPRESSION: this.foo("a")
// RESULT: 2: I

// SKIP_SYNTHETIC_METHODS: false