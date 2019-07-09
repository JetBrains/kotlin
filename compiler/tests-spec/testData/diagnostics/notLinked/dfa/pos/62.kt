// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 62
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_1(x: Any) {
    x as Boolean
    val y = <!NO_ELSE_IN_WHEN!>when<!>(x) {
        true -> "true"
        false -> "false"
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_2(x: Any) {
    x as Boolean?
    val y = <!NO_ELSE_IN_WHEN!>when<!>(x) {
        true -> "true"
        false -> "false"
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> "false"
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_3(x: Any?) {
    x as Boolean?
    val y = <!NO_ELSE_IN_WHEN!>when<!>(x) {
        true -> "true"
        false -> "false"
        null -> "false"
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?): String {
    x as Boolean
    return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        true -> "true"
        false -> "false"
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_5(x: Any): String {
    x as Boolean?
    return <!NO_ELSE_IN_WHEN!>when<!>(x) {
        true -> "true"
        false -> "false"
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_6(x: Any) {
    if (x is Boolean) {
        val y = <!NO_ELSE_IN_WHEN!>when<!>(x) {
            true -> "true"
            false -> "false"
        }
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_7(x: Any) {
    if (x is Boolean<!USELESS_NULLABLE_CHECK!>?<!>) {
        val y = <!NO_ELSE_IN_WHEN!>when<!>(x) {
            true -> "true"
            false -> "false"
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> "false"
        }
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_8(x: Any?) {
    val y: Any
    when (x) {
        is Boolean? -> y = <!NO_ELSE_IN_WHEN!>when<!>(x) {
            true -> "true"
            false -> "false"
            null -> "false"
        }
        else -> y = Any()
    }

}

// TESTCASE NUMBER: 9
fun case_9(x: Any?): String {
    if (x is Boolean) {
        return when (<!DEBUG_INFO_SMARTCAST!>x<!>) {
            true -> "true"
            false -> "false"
        }
    }
    return ""
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-17694
 */
fun case_10(x: Any): String {
    if (x is Boolean<!USELESS_NULLABLE_CHECK!>?<!>) {
        return <!NO_ELSE_IN_WHEN!>when<!>(x) {
            true -> "true"
            false -> "false"
        }
    }
    return ""
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30903
 */
fun case_11(x: Any?): String? {
    if (x is Nothing?) {
        return <!NO_ELSE_IN_WHEN!>when<!>(x) {
            null -> null
        }
    }
    return ""
}

/*
 * TESTCASE NUMBER: 12
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30903
 */
fun case_12(x: Any?): String? {
    if (x == null) {
        return <!NO_ELSE_IN_WHEN!>when<!>(<!DEBUG_INFO_CONSTANT!>x<!>) {
            null -> null
        }
    }
    return ""
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30903
 */
fun case_13(x: Any?): String? {
    if (x === null) {
        return <!NO_ELSE_IN_WHEN!>when<!>(<!DEBUG_INFO_CONSTANT!>x<!>) {
            null -> null
        }
    }
    return ""
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30903
 */
fun case_14(x: Any?): String? {
    x as Nothing?
    return <!NO_ELSE_IN_WHEN!>when<!>(x) {
        null -> null
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: Any) {
    x as EnumClass
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        EnumClass.NORTH -> 1
        EnumClass.SOUTH -> 2
        EnumClass.WEST -> 3
        EnumClass.EAST -> 4
    }
}

// TESTCASE NUMBER: 16
fun case_16(x: Any) {
    if (x is EnumClass<!USELESS_NULLABLE_CHECK!>?<!>) {
        val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            EnumClass.NORTH -> 1
            EnumClass.SOUTH -> 2
            EnumClass.WEST -> 3
            EnumClass.EAST -> 4
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> 5
        }
    }
}

// TESTCASE NUMBER: 17
fun case_17(x: Any?) {
    x as EnumClass?
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        EnumClass.NORTH -> 1
        EnumClass.SOUTH -> 2
        EnumClass.WEST -> 3
        EnumClass.EAST -> 4
        null -> 5
    }
}

// TESTCASE NUMBER: 18
fun case_18(x: EnumClass?): Int {
    x!!
    return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        EnumClass.NORTH -> 1
        EnumClass.SOUTH -> 2
        EnumClass.WEST -> 3
        EnumClass.EAST -> 4
    }
}

// TESTCASE NUMBER: 19
fun case_19(x: Any): Int {
    if (x is EnumClass<!USELESS_NULLABLE_CHECK!>?<!>) {
        return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            EnumClass.NORTH -> 1
            EnumClass.SOUTH -> 2
            EnumClass.WEST -> 3
            EnumClass.EAST -> 4
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> 5
        }
    }
    return 0
}

// TESTCASE NUMBER: 20
fun case_20(x: Any?) {
    x as EnumClass
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        EnumClass.NORTH -> 1
        EnumClass.SOUTH -> 2
        EnumClass.WEST -> 3
        EnumClass.EAST -> 4
    }
}

// TESTCASE NUMBER: 21
fun case_21(x: EnumClass?): Int {
    if (x !== null) {
        return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            EnumClass.NORTH -> 1
            EnumClass.SOUTH -> 2
            EnumClass.WEST -> 3
            EnumClass.EAST -> 4
        }
    }
    return 0
}

// TESTCASE NUMBER: 22
fun case_22(x: Boolean?) {
    x!!
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        true -> "true"
        false -> "false"
    }
}

// TESTCASE NUMBER: 23
fun case_23(x: Any?) {
    x as Boolean?
    if (x != null) {
        val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            true -> "true"
            false -> "false"
        }
    }
}

// TESTCASE NUMBER: 24
fun case_24(x: Any) {
    x as SealedClass
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        is SealedChild1 -> 1
        is SealedChild2 -> 2
        is SealedChild3 -> 3
    }
}

// TESTCASE NUMBER: 25
fun case_25(x: Any) {
    if (x is SealedClass<!USELESS_NULLABLE_CHECK!>?<!>) {
        val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            is SealedChild1 -> 1
            is SealedChild2 -> 2
            is SealedChild3 -> 3
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> 5
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(x: Any?) {
    x as SealedClass?
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        is SealedChild1 -> 1
        is SealedChild2 -> 2
        is SealedChild3 -> 3
        null -> 5
    }
}

// TESTCASE NUMBER: 27
fun case_27(x: SealedClass?): Int {
    x!!
    return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        is SealedChild1 -> 1
        is SealedChild2 -> 2
        is SealedChild3 -> 3
    }
}

// TESTCASE NUMBER: 28
fun case_28(x: Any): Int {
    if (x is SealedClass<!USELESS_NULLABLE_CHECK!>?<!>) {
        return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            is SealedChild1 -> 1
            is SealedChild2 -> 2
            is SealedChild3 -> 3
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> 5
        }
    }
    return 0
}

// TESTCASE NUMBER: 29
fun case_29(x: Any?) {
    x as SealedClass
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        is SealedChild1 -> 1
        is SealedChild2 -> 2
        is SealedChild3 -> 3
    }
}

// TESTCASE NUMBER: 30
fun case_30(x: SealedClass?): Int {
    if (x !== null) {
        return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            is SealedChild1 -> 1
            is SealedChild2 -> 2
            is SealedChild3 -> 3
        }
    }
    return 0
}

// TESTCASE NUMBER: 31
fun case_31(x: Any) {
    x as SealedClassWithObjects
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        SealedWithObjectsChild1 -> 1
        SealedWithObjectsChild2 -> 2
        SealedWithObjectsChild3 -> 3
    }
}

// TESTCASE NUMBER: 32
fun case_32(x: Any) {
    if (x is SealedClassWithObjects<!USELESS_NULLABLE_CHECK!>?<!>) {
        val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            SealedWithObjectsChild1 -> 1
            SealedWithObjectsChild2 -> 2
            SealedWithObjectsChild3 -> 3
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> 5
        }
    }
}

// TESTCASE NUMBER: 33
fun case_33(x: Any?) {
    x as SealedClassWithObjects?
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        SealedWithObjectsChild1 -> 1
        SealedWithObjectsChild2 -> 2
        SealedWithObjectsChild3 -> 3
        null -> 5
    }
}

// TESTCASE NUMBER: 34
fun case_34(x: SealedClassWithObjects?): Int {
    x!!
    return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        SealedWithObjectsChild1 -> 1
        SealedWithObjectsChild2 -> 2
        SealedWithObjectsChild3 -> 3
    }
}

// TESTCASE NUMBER: 35
fun case_35(x: Any): Int {
    if (x is SealedClassWithObjects<!USELESS_NULLABLE_CHECK!>?<!>) {
        return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            SealedWithObjectsChild1 -> 1
            SealedWithObjectsChild2 -> 2
            SealedWithObjectsChild3 -> 3
            <!SENSELESS_NULL_IN_WHEN!>null<!> -> 5
        }
    }
    return 0
}

// TESTCASE NUMBER: 36
fun case_36(x: Any?) {
    x as SealedClassWithObjects
    val y = when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
        SealedWithObjectsChild1 -> 1
        SealedWithObjectsChild2 -> 2
        SealedWithObjectsChild3 -> 3
    }
}

// TESTCASE NUMBER: 37
fun case_37(x: SealedClassWithObjects?): Int {
    if (x !== null) {
        return when(<!DEBUG_INFO_SMARTCAST!>x<!>) {
            SealedWithObjectsChild1 -> 1
            SealedWithObjectsChild2 -> 2
            SealedWithObjectsChild3 -> 3
        }
    }
    return 0
}
