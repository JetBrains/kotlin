// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun stringF(): String = ""
fun intF(): Int = 10
fun unitF(): Unit = Unit
fun nsf(): String? = "null"
@IgnorableReturnValue fun insf(): String? = "null"

fun ifCondition() {
    <!RETURN_VALUE_NOT_USED!>intF() > 0<!> // not used
    val y = intF() > 0 // used
    if (intF() > 0) unitF() else unitF() // used
    println(intF() > 0) // used
}

fun whenCondition() {
    when (intF()) {
        0 -> unitF()
    }

    when (val x = intF()) {
        0 -> <!UNUSED_EXPRESSION!>x<!>
    }

    when (intF()) {
        intF() -> unitF()
    }

    when (intF()) {
        intF() -> <!RETURN_VALUE_NOT_USED!>intF()<!> // only part after -> should be reported unused
    }
}

fun ifBranches() {
    val x = if (intF() > 0) intF() else 0 // used
    if (intF() > 0) <!RETURN_VALUE_NOT_USED!>intF()<!> else <!UNUSED_EXPRESSION!>0<!> // unused
}

fun ifBranches2(cond: Boolean): String? {
    if (cond) {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
    } else {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
    }

    return if (cond) {
        val x = intF() // unrelated
        stringF()
    } else {
        <!RETURN_VALUE_NOT_USED!>intF()<!> // unused
        nsf()
    }
}

fun tryCatch() {
    val x = try {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
        nsf()
    } catch (e: Exception) {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
        "x"
    } finally {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
    }

    try {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
    } catch (e: Exception) {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
    }

    try {
        val used = stringF()
    } catch (e: Exception) {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
    } finally {
        unitF() // Unit, OK to discard
    }
}

fun typicalError(cond: Boolean): String {
    if (cond) {
        <!RETURN_VALUE_NOT_USED!>nsf()<!> // value unused
    } else {
        return stringF()
    }
    return "default"
}

fun elvis(): String {
    <!RETURN_VALUE_NOT_USED!>nsf()<!> ?: unitF() // OK to discard Unit
    <!RETURN_VALUE_NOT_USED!>nsf()<!> ?: <!RETURN_VALUE_NOT_USED!>stringF()<!> // unused
    insf() ?: <!RETURN_VALUE_NOT_USED!>stringF()<!> // unused
    <!RETURN_VALUE_NOT_USED!>nsf()<!> ?: return ""
    insf() ?: return ""
    val x = nsf() ?: "" // used
    return nsf() ?: stringF()
}
