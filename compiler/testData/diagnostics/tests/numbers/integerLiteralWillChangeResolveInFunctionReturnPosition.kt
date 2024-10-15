// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-55358

fun test_0() = 1 + 2

fun test_1(): Byte = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 2<!>

fun test_2(b: Boolean): Byte {
    if (b) return <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 2<!>
    return <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>3 + 4<!>
}

fun test_4(): Byte = run { <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 2<!> }

fun test_5() = run<Byte> { <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 + 2<!> }

fun test_6() = runWithByte { <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE, TYPE_MISMATCH!>1 + 2<!> }

fun <R> run(block: () -> R): R = block()
fun runWithByte(block: () -> Byte): Byte = block()
