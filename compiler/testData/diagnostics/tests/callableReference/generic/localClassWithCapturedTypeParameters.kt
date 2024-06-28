// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68350

fun <T, K> test() {
    class InnerClass(var x: Int, val y: T)

    val list: List<InnerClass> = listOf()
    list.sortedBy(InnerClass::x)
}
