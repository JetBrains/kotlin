fun test_1() {
    val list = buildList {
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

fun <E> myBuildList(@<!OPT_IN_USAGE_ERROR!>BuilderInference<!> builderAction: MutableList<E>.() -> Unit): List<E> {
    return ArrayList<E>().apply(builderAction)
}

fun takeList(list: List<String>) {}