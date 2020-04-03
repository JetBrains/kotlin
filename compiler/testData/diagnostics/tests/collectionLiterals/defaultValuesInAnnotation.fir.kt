// !WITH_NEW_INFERENCE
// !LANGUAGE: +ArrayLiteralsInAnnotations

annotation class Foo(
        val a: Array<String> = ["/"],
        val b: Array<String> = [],
        val c: Array<String> = ["1", "2"]
)

annotation class Bar(
        val a: Array<String> = [' '],
        val b: Array<String> = ["", <!ILLEGAL_CONST_EXPRESSION!>''<!>],
        val c: Array<String> = [1]
)

annotation class Base(
        val a0: IntArray = [],
        val a1: IntArray = [1],
        val b1: FloatArray = [1f],
        val b0: FloatArray = []
)

annotation class Err(
        val a: IntArray = [1L],
        val b: Array<String> = [1]
)