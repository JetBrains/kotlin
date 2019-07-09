package nestedInlineArguments

fun main(args: Array<String>) {
    val list1 = listOf("a")
    val list2 = listOf(Bar())
    list1.mapTest { list2.foo(1) }
}

inline fun <reified T: Bar> List<T>.foo(key: Int): T? {
    // EXPRESSION: it.i
    // RESULT: 1: I
    //Breakpoint! (lambdaOrdinal = 1)
    return this.firstOrNull { it.i == key }
}

open class Foo
open class Bar {
    val i = 1
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T, R> Iterable<T>.mapTest(transform: (T) -> R): List<R> {
    return mapToTest(ArrayList<R>(), transform)
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapToTest(destination: C, transform: (T) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}