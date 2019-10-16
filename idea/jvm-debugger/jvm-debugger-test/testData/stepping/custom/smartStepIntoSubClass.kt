package smartStepIntoSubClass

fun main(args: Array<String>) {
    val x: Base = Impl()
    // SMART_STEP_INTO_BY_INDEX: 1
    //Breakpoint!
    x.foo(args.toString())
}

open class Base {
    open fun foo(s: String) {
        val a = 1
    }
}

class Impl: Base() {
    override fun foo(s: String) {
        val b = 1
    }
}