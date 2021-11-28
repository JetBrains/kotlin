// WITH_STDLIB
fun foo() {
    var list1 = java.util.Collections.emptyList<String>()
    val list2 = listOf("b")
    <!ASSIGNED_VALUE_IS_NEVER_READ!>list1<!> = list1 + list2
}
