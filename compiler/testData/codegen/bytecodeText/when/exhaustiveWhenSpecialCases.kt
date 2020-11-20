// IGNORE_BACKEND_FIR: JVM_IR

enum class A { V1 }

fun testVariableAssignment_throws(a: A) {
    val x: Int
    when (a) {
        A.V1 -> x = 11
        // else -> throw
    }
}

fun testStatement_empty(a: A) {
    when (a) {
        A.V1 -> 1
        // else -> {}
    }
}

fun testParenthesized_throwsJvm(a: A) {
    (when (a) {
        A.V1 -> 1
        // JVM: else -> throw
        // JVM_IR: else -> {}
    })
}

fun testAnnotated_throwsJvm(a: A) {
    @Suppress("") when (a) {
        A.V1 -> 1
        // JVM: else -> throw
        // JVM_IR: else -> {}
    }
}

fun testExpression_throws(a: A) =
    when (a) {
        A.V1 -> 1
        // else -> throw
    }

fun testIfTheElseStatement_empty(a: A, flag: Boolean) {
    if (flag)
        0
    else {
        when (a) {
            A.V1 -> 1
            // else -> {}
        }
    }
}

fun testIfTheElseParenthesized_throwsJvm(a: A, flag: Boolean) {
    (if (flag)
        0
    else {
        when (a) {
            A.V1 -> 1
            // JVM: else -> throw
            // JVM_IR: else -> {}
        }
    })
}

fun testIfTheElseAnnotated_throwsJvm(a: A, flag: Boolean) {
    @Suppress("")
    if (flag)
        0
    else {
        when (a) {
            A.V1 -> 1
            // JVM: else -> throw
            // JVM_IR: else -> {}
        }
    }
}

fun testLambdaResultExpression_throws(a: A) {
    {
        when (a) {
            A.V1 -> 1
            // else -> throw
        }
    }()
}

// JVM_TEMPLATES
// 7 ATHROW

// JVM_IR_TEMPLATES
// 3 ATHROW