// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN

inline fun <R> contractlessRun(block: () -> R): R {
    return block()
}

fun initInGuard(x: Any) {

    val value100: Int

    when (x) {
        is String <!UNSUPPORTED_FEATURE!>if run { value100 = 42; true }<!> -> {}
        is Boolean -> {}
    }

    <!UNINITIALIZED_VARIABLE!>value100<!>
}

fun initInGuardReassignmentInGuard(x: Any) {

    val value500: Int

    when (x) {
        is String <!UNSUPPORTED_FEATURE!>if run { value500 = 42; true }<!> -> {}
        !is String <!UNSUPPORTED_FEATURE!>if run { value500 = 142; true }<!> -> {}
    }

    <!UNINITIALIZED_VARIABLE!>value500<!>
}

fun initInConditionReassignmentInGuard(x: Any) {

    val value510: Int

    when (x) {
        run { value510 = 42; true } <!UNSUPPORTED_FEATURE!>if run { value510 = 42; true }<!> -> {}
    }

    value510
}
fun initInGuardReassignmentInBody(x: Any) {

    val value520: Int

    when (x) {
        is String <!UNSUPPORTED_FEATURE!>if run { value520 = 42; true }<!> -> run { value520 = 42; true }
    }

    <!UNINITIALIZED_VARIABLE!>value520<!>
}

fun initInConditionReassignmentInGuardAndBody(x: Any) {
    val value530: Int

    when (x) {
        run { value530 = 42; true } <!UNSUPPORTED_FEATURE!>if x is Boolean<!> -> {}
        is String <!UNSUPPORTED_FEATURE!>if run { value530 = 42; true }<!> -> {}
        !is String <!UNSUPPORTED_FEATURE!>if x is Int<!> -> run { <!VAL_REASSIGNMENT!>value530<!> = 42; true }
    }

    value530
}

fun initInGuardBlock(x: Any) {

    val value900: Int

    when(x) {
        is String <!UNSUPPORTED_FEATURE!>if contractlessRun { value900 = 42; true }<!> -> {}
        is Boolean -> {}
    }

    <!UNINITIALIZED_VARIABLE!>value900<!>
}

class ClassPropInitialization(x: Any) {

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val prop1: Int<!>

    init {
        when(x) {
            is String <!UNSUPPORTED_FEATURE!>if run { prop1 = 42; true }<!> -> {}
            is Boolean -> {}
        }
    }
}
