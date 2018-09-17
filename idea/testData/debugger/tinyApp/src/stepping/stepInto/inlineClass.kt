package inlineClass

interface A {
    fun foo()
}

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class B(val a: String) : A {
    override fun foo() {
        println("foo")
    }
}

fun main(args: Array<String>) {
    val b = B("")
    //Breakpoint!
    b.foo()
}

// STEP_INTO: 1