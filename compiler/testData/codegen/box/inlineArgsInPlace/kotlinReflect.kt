// FULL_JDK
// WITH_STDLIB

interface Foo {
    val foos: List<Foo>
}

inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

fun faultyLvt() {
    sequenceOf<Foo>().firstIsInstanceOrNull<Foo>()?.foos.orEmpty()

    listOf<Foo>().map { it }
}

fun box(): String {
    faultyLvt()
    return "OK"
}