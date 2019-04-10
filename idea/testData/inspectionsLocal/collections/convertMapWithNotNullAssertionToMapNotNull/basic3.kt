// WITH_RUNTIME

fun baz(list: List<Int?>) {
    with(list) {
        <caret>map {
            it!!
        }
    }
}