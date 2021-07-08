class DropDownComponent<T : Any>(val initialValues: List<T>)

fun test(strings: List<String>) {
    val dropDown = DropDownComponent(
        initialValues = <!EXPERIMENTAL_API_USAGE_ERROR!>buildList<!> {
            addAll(strings)
        }
    )
}
