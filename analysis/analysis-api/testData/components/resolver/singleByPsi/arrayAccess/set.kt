// WITH_STDLIB
fun a () {
    val list = mutableListOf(1)
    list<caret>[0] = 1

    if (true) list<caret_singleIfStatement>[0] = 1

    when { else -> list<caret_singleWhenStatement>[0] = 1 }
}
