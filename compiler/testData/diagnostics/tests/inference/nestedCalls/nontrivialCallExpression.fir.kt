package a

fun foo(l: List<Int>): Int = l.get(0)

fun <T> emptyList(): List<T> = throw Exception()

fun <T: Any> makeNullable(t: T): T? = null

fun bar(i: Int) = i
fun bar(a: Any) = a

fun test(array: Array<Int>) {
    bar(array[foo(emptyList())])

    bar(foo(emptyList()) + foo(a.emptyList()))

    bar(makeNullable(foo(emptyList())) ?: 0)
}