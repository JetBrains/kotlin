// WITH_STDLIB
// ISSUE: KT-68350

fun <T, K> test() {
    class InnerClass(var x: Int, val y: T)

    val list: List<InnerClass> = listOf()
    list.<!CANNOT_INFER_PARAMETER_TYPE!>sortedBy<!>(InnerClass::<!INAPPLICABLE_CANDIDATE!>x<!>)
}
