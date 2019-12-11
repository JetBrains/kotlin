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
    [1, 2] checkType { <!UNRESOLVED_REFERENCE!>_<!><Array<Int>>() }
    [""] checkType { <!UNRESOLVED_REFERENCE!>_<!><Array<String>>() }

    val f: IntArray = [1]
    [f] checkType { <!UNRESOLVED_REFERENCE!>_<!><Array<IntArray>>() }

    [1, ""] checkType { <!UNRESOLVED_REFERENCE!>_<!><Array<Any>>() }
}