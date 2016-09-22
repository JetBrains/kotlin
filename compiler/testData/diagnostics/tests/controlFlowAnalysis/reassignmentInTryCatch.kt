// KT-13612 related tests (reassignment in try-catch-finally)

fun f1() {
    val n: Int
    try {
        <!UNUSED_VALUE!>n =<!> 1
        throw Exception()
    }
    catch (e: Exception) {
        // KT-13612: reassignment
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}

fun f2() {
    val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>n<!>: Int
    try {
        <!UNUSED_VALUE!>n =<!> 1
        throw Exception()
    }
    finally {
        <!UNUSED_VALUE!><!VAL_REASSIGNMENT!>n<!> =<!> 2
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
        <!UNUSED_VALUE!>n =<!> 1
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
        <!UNUSED_VALUE!>n =<!> 1
        j(flag)
    }
    finally {
        <!VAL_REASSIGNMENT!>n<!> = 2
    }
    n.hashCode()
}
