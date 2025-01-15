// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class Test {
    companion object Named {
        context(c: Named)
        fun foo() {}

        context(a: Named)
        val b: String
            get() = ""
    }

    fun usage() {
        foo()
        b
    }
}

fun usageOutside() {
    context(Test.Named) {
        Test.Named.foo()
        Test.foo()
        Test.Named.b
        Test.b
    }
    Test.Named.<!NO_CONTEXT_ARGUMENT!>foo<!>()
    Test.<!NO_CONTEXT_ARGUMENT!>foo<!>()
    Test.Named.<!NO_CONTEXT_ARGUMENT!>b<!>
    Test.<!NO_CONTEXT_ARGUMENT!>b<!>
}

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)