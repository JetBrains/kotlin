// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION

fun ifExpr() = try {
    if (true) 2
    <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 2
} catch (e: Exception) {
    if (true) 3
    <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 3
} catch (e: Throwable) {
    if (true) 4
    <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 4
} finally {
    if (true) 5
    if (true) 5
}

fun ifBlock() = try {
    if (true) {
        2
    }
    <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {
        2
    }
} catch (e: Exception) {
    if (true) {
        3
    }
    <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {
        3
    }
} catch (e: Throwable) {
    if (true) {
        4
    }
    <!INVALID_IF_AS_EXPRESSION!>if<!> (true) {
        4
    }
} finally {
    if (true) {
        5
    }
    if (true) {
        5
    }
}

fun whenExpr() = try {
    when {
        true -> 2
    }
    <!NO_ELSE_IN_WHEN!>when<!> {
        true -> 2
    }
} catch (e: Exception) {
    when {
        true -> 3
    }
    <!NO_ELSE_IN_WHEN!>when<!> {
        true -> 3
    }
} catch (e: Throwable) {
    when {
        true -> 4
    }
    <!NO_ELSE_IN_WHEN!>when<!> {
        true -> 4
    }
} finally {
    when {
        true -> 5
    }
    when {
        true -> 5
    }
}

fun whenBlock() = try {
    when {
        true -> {
            2
        }
    }
    <!NO_ELSE_IN_WHEN!>when<!> {
        true -> {
            2
        }
    }
} catch (e: Exception) {
    when {
        true -> {
            3
        }
    }
    <!NO_ELSE_IN_WHEN!>when<!> {
        true -> {
            3
        }
    }
} catch (e: Throwable) {
    when {
        true -> {
            4
        }
    }
    <!NO_ELSE_IN_WHEN!>when<!> {
        true -> {
            4
        }
    }
} finally {
    when {
        true -> {
            5
        }
    }
    when {
        true -> {
            5
        }
    }
}


fun ifExpr2(): Any {
    try {
        if (true) 2
        if (true) 2
    } catch (e: Exception) {
        if (true) 3
        if (true) 3
    } catch (e: Throwable) {
        if (true) 4
        if (true) 4
    } finally {
        if (true) 5
        if (true) 5
    }
    
    return try {
        if (true) 2
        <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 2
    } catch (e: Exception) {
        if (true) 3
        <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 3
    } catch (e: Throwable) {
        if (true) 4
        <!INVALID_IF_AS_EXPRESSION!>if<!> (true) 4
    } finally {
        if (true) 5
        if (true) 5
    }
}


fun ifExpr3() {
    try {
        if (true) 2
        if (true) 2
    } catch (e: Exception) {
        if (true) 3
        if (true) 3
    } catch (e: Throwable) {
        if (true) 4
        if (true) 4
    } finally {
        if (true) 5
        if (true) 5
    }
    
    try {
        if (true) 2
        if (true) 2
    } catch (e: Exception) {
        if (true) 3
        if (true) 3
    } catch (e: Throwable) {
        if (true) 4
        if (true) 4
    } finally {
        if (true) 5
        if (true) 5
    }
}
