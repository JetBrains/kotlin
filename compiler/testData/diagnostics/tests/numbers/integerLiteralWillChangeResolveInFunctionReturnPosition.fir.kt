// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-55358

fun test_0() = 1 + 2

fun test_1(): Byte = <!RETURN_TYPE_MISMATCH!>1 + 2<!>

fun test_2(b: Boolean): Byte {
    if (b) return <!RETURN_TYPE_MISMATCH!>1 + 2<!>
    return <!RETURN_TYPE_MISMATCH!>3 + 4<!>
}

fun test_4(): Byte = run { <!RETURN_TYPE_MISMATCH!>1 + 2<!> }

fun test_5() = run<Byte> { <!RETURN_TYPE_MISMATCH!>1 + 2<!> }

fun test_6() = runWithByte { <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>1 + 2<!> }

fun <R> run(block: () -> R): R = block()
fun runWithByte(block: () -> Byte): Byte = block()
