// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNSUPPORTED

fun test() {
    val a = []
    val b: Array<Int> = []
    val c = [1, 2]
    val d: Array<Int> = [1, 2]
    val e: Array<String> = [1]

    val f: IntArray = [1, 2]
    val g = [f]
}

fun check() {
    [1, 2] <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Array<Int>>() }
    [""] <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Array<String>>() }

    val f: IntArray = [1]
    [f] <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Array<IntArray>>() }

    [1, ""] <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><Array<Any>>() }
}
