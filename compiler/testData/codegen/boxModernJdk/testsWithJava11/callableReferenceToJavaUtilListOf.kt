// TARGET_BACKEND: JVM
// WITH_STDLIB
// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals

fun take0(foo: Function0<List<Int>>) { }
fun <T> take1(foo: Function1<T, List<Int>>) { }

fun box(): String {
    take0(java.util.List::of)
    take1(java.util.List::of)
    val ref: (Array<String>) -> List<String> = java.util.List::of
    return ref(["OK"])[0]
}
