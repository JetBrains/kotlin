class DropDownComponent<T : Any>(val initialValues: List<T>)

fun test(strings: List<String>) {
    val dropDown = DropDownComponent(
        initialValues = buildList {
            addAll(strings)
        }
    )
}
