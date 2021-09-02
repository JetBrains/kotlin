class DropDownComponent<T : Any>(val initialValues: List<T>)

fun test(strings: List<String>) {
    val dropDown = DropDownComponent(
        initialValues = <!OPT_IN_USAGE_ERROR!>buildList<!> {
            addAll(strings)
        }
    )
}
