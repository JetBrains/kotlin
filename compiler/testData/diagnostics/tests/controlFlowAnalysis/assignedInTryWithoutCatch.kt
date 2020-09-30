fun assignedInTry() {
    val a: Int
    try {
        a = 42
    } finally {
    }
    a.hashCode()
}

fun sideEffectBeforeAssignmentInTry(s: Any) {
    val a: Int
    try {
        s as String // Potential cast exception
        a = 42
    } finally {
    }
    a.hashCode()
}

fun assignedInTryAndFinally() {
    val a: Int
    try {
        <!UNUSED_VALUE!>a =<!> 42
    } finally {
        <!VAL_REASSIGNMENT!>a<!> = 41
    }
    a.hashCode()
}

fun sideEffectBeforeAssignmentInTryButNotFinally(s: Any) {
    val a: Int
    try {
        s as String // Potential cast exception
        <!UNUSED_VALUE!>a =<!> 42
    } finally {
        <!VAL_REASSIGNMENT!>a<!> = 41
    }
    a.hashCode()
}
