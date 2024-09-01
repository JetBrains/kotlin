// WITH_STDLIB
// ISSUE: KT-50293

fun main() {
    val list = buildList {
        add("one")
        add("two")

        val secondParameter = get(1)
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OVERLOAD_RESOLUTION_AMBIGUITY, OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>println<!>(<!STUB_TYPE_IN_ARGUMENT_CAUSES_AMBIGUITY!>secondParameter<!>)
    }
}