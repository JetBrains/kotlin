fun error(): Nothing = null!!

fun test0(): String {
    null ?: return ""
}

fun test1(): String {
    run { null } ?: return ""
}

fun test2(): String {
    run<Nothing?> { null } ?: return ""
}

fun test3(): String {
    run { error() } <!UNREACHABLE_CODE, USELESS_ELVIS!>?: return ""<!>
}

fun test4(): String {
    run { run { null } ?: return "" }
}
