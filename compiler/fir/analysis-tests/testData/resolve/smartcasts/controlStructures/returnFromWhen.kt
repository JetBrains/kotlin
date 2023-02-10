// ISSUE: KT-52262

fun test_1(name: String?) {
    when (name) {
        null -> return
    }
    name.length
}

fun test_2(name: String?) {
    when (val s = name) {
        null -> return
    }
    name.length
}
