package foo

fun foo(a: Any) = foo(1)

fun foo(i: Int) = "foo(Int)"

fun main(args: Array<String>) {
    println(foo(""))
}