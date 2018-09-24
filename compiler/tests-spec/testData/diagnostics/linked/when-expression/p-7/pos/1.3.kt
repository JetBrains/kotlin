// !WITH_CLASSES
// !WITH_SEALED_CLASSES
// !WITH_OBJECTS

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 3
 DESCRIPTION: 'When' with bound value and enumaration of type test conditions.
 */

// CASE DESCRIPTION: 'When' with type test condition on the various basic types.
fun case_1(value: Any) = when (value) {
    is Int -> {}
    is Float, is Char, is Boolean -> {}
    is String -> {}
    else -> {}
}

// CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various nullable basic types.
fun case_2(value: Any?) = when (value) {
    is Float, is Char, is _SealedClass? -> "" // if value is null then this branch will be executed
    is Double, is Boolean, is _ClassWithCompanionObject.Companion -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various nullable basic types (two nullable type check in the different branches).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_3(value: Any?) = when (value) {
    is Float, is Char, is Int? -> "" // if value is null then this branch will be executed
    is _SealedChild2, is Boolean?, is String -> "" // redundant nullable type check
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various nullable basic types (two nullable type check in the one branch).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_4(value: Any?) = when (value) {
    is Float, is Char?, is Int? -> "" // double nullable type check in the one branch
    is _SealedChild1, is Boolean, is String -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various nullable basic types (two nullable type check).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_5(value: Any?): String {
    when (value) {
        is Float, is Char?, is Int -> return ""
        is Double, is _EmptyObject, is String -> return ""
        null -> return "" // null-check redundant
        else -> return ""
    }
}

/*
 CASE DESCRIPTION: 'When' with 'else' branch and type test condition on the various nullable basic types (two different nullable type check in the one branch).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_6(value: Any?): String {
    when (value) {
        is Float, is Char?, null, is Int -> return "" // double nullable type check in the one branch
        is Double, is _EmptyObject, is String -> return ""
        else -> return ""
    }
}
