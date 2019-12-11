// !LANGUAGE: -ArrayLiteralsInAnnotations
annotation class Foo(
        val a: IntArray = [],
        val b: FloatArray = [1f, 2f],
        val c: Array<String> = ["/"]
)

@Foo
fun test1() {}

@Foo(a = [1, 2], c = ["a"])
fun test2() {}

@Foo([1], [3f], ["a"])
fun test3() {}

fun test4() {
    [1, 2]
}
