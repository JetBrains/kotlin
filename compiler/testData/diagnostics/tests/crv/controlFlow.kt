// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +WhenGuards

@file:MustUseReturnValues

fun stringF(): String = ""
fun intF(): Int = 10
fun booleanF(): Boolean = true
fun unitF(): Unit = Unit
fun nsf(): String? = "null"
@IgnorableReturnValue fun insf(): String? = "null"

fun ifCondition() {
    intF() > 0 // not used
    val y = intF() > 0 // used
    if (intF() > 0) unitF() else unitF() // used
    println(intF() > 0) // used
}

fun whenCondition() {
    when (intF()) {
        0 -> unitF()
    }

    when (val x = intF()) {
        0 -> x
    }

    when (intF()) {
        intF() -> unitF()
    }

    when (intF()) {
        intF() -> intF() // only part after -> should be reported unused
    }

    when(intF()) {
        1, intF() -> unitF()
    }

    when(intF()) {
        1 if booleanF() -> unitF()
        else if booleanF() -> unitF()
    }

    when {
        booleanF() -> unitF()
    }
}

fun ifBranches() {
    val x = if (intF() > 0) intF() else 0 // used
    if (intF() > 0) intF() else 0 // unused
}

fun ifBranches2(cond: Boolean): String? {
    if (cond) {
        stringF()
    } else {
        nsf()
    }

    return if (cond) {
        val x = intF() // unrelated
        stringF()
    } else {
        intF() // unused
        nsf()
    }
}

fun tryCatch() {
    val x = try {
        stringF()
        nsf()
    } catch (e: Exception) {
        stringF()
        "x"
    } finally {
        nsf()
        stringF()
    }

    try {
        stringF()
    } catch (e: Exception) {
        nsf()
    }

    try {
        val used = stringF()
    } catch (e: Exception) {
        nsf()
    } finally {
        unitF() // Unit, OK to discard
    }
}

fun typicalError(cond: Boolean): String {
    if (cond) {
        nsf() // value unused
    } else {
        return stringF()
    }
    return "default"
}

fun elvis(): String {
    nsf() ?: unitF() // OK to discard Unit
    nsf() ?: stringF() // unused
    insf() ?: stringF() // unused
    nsf() ?: return ""
    insf() ?: return ""
    val x = nsf() ?: "" // used
    return nsf() ?: stringF()
}

/* GENERATED_FIR_TAGS: andExpression, annotationUseSiteTargetFile, comparisonExpression, disjunctionExpression,
elvisExpression, equalityExpression, functionDeclaration, guardCondition, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, tryExpression, whenExpression, whenWithSubject */
