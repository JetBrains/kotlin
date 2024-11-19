// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// KT-13612 related tests (reassignment in try-catch-finally)

fun f1() {
    val n: Int
    try {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>n<!> = 1
        throw Exception()
    }
    catch (e: Exception) {
        // KT-13612: reassignment
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}

fun f2() {
    val n: Int
    try {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>n<!> = 1
        throw Exception()
    }
    finally {
        <!ASSIGNED_VALUE_IS_NEVER_READ, VAL_REASSIGNMENT!>n<!> = 2
    }
    <!UNREACHABLE_CODE!>n.hashCode()<!>
}

fun g1(flag: Boolean) {
    val n: Int
    try {
        if (flag) throw Exception()
        n = 1
    }
    catch (e: Exception) {
        // KT-13612: ? reassignment or definite assignment ?
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}

fun g2(flag: Boolean) {
    val n: Int
    try {
        if (flag) throw Exception()
        <!ASSIGNED_VALUE_IS_NEVER_READ!>n<!> = 1
    }
    finally {
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}

fun h1(flag: Boolean) {
    val n = try {
        if (flag) throw Exception()
        1
    }
    catch (e: Exception) {
        2
    }
    n.hashCode()
}

fun h2(flag: Boolean) {
    val n = try {
        if (flag) throw Exception()
        1
    }
    finally {
        <!UNUSED_EXPRESSION!>2<!>
    }
    n.hashCode()
}

fun j(flag: Boolean) {
    if (flag) throw Exception()
}

fun k1(flag: Boolean) {
    val n: Int
    try {
        n = 1
        j(flag)
    }
    catch (e: Exception) {
        // KT-13612: reassignment
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}

fun k2(flag: Boolean) {
    val n: Int
    try {
        <!ASSIGNED_VALUE_IS_NEVER_READ!>n<!> = 1
        j(flag)
    }
    finally {
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}
