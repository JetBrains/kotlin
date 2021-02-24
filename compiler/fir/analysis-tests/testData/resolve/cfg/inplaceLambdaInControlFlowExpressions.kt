// !DUMP_CFG

fun <K> materialize(): K = null!!

fun test_1() {
    val x = if (true) {
        run { materialize() }
    } else {
        ""
    }
}

fun test_2() {
    val x = try {
        run {
            materialize()
        }
    } catch (e: Exception) {
        ""
    }
}

fun test_3() {
    val x: String = run { materialize() }!!
}
