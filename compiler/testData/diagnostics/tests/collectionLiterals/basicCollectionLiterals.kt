// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNSUPPORTED

fun test() {
    val a = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>[]<!>
    val b: Array<Int> = []
    val c = [1, 2]
    val d: Array<Int> = [1, 2]
    val e: Array<String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>[1]<!>

    val f: IntArray = [1, 2]
    val g = [f]
}

fun check() {
    [1, 2] checkType { _<Array<Int>>() }
    [""] checkType { _<Array<String>>() }

    val f: IntArray = [1]
    [f] checkType { _<Array<IntArray>>() }

    [1, ""] checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><Array<Any>>() }
}
