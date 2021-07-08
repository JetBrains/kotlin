fun test_1() {
    val list = <!EXPERIMENTAL_API_USAGE_ERROR!>buildList<!> {
        add("")
    }
    takeList(list)
}

fun test_2() {
    val list = myBuildList {
        add("")
    }
    takeList(list)
}

fun <E> myBuildList(@<!EXPERIMENTAL_API_USAGE_ERROR!>BuilderInference<!> builderAction: MutableList<E>.() -> Unit): List<E> {
    return ArrayList<E>().apply(builderAction)
}

fun takeList(list: List<String>) {}