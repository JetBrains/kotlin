// RUN_PIPELINE_TILL: BACKEND

sealed class Example {
    object Foo: Example()
    class Bar: Example() {
        override fun equals(other: Any?) = true
    }
}

fun tainted(e: Example): Int =
    when (e) {
        Example.Foo -> 1
        is Example.Bar -> 2
    }