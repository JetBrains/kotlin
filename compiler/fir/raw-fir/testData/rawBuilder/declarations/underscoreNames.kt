fun foo() {
    val _ = 1
    val `_` = 2
    when (val _ = 3) { else -> {} }
    when (val `_` = 4) { else -> {} }
    for (_ in 1..2) {}
    for (`_` in 1..2) {}
    val (_, `_`) = 1 to 2
    for ((_, `_`) in listOf(1 to 2)) {}
}
