// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-35177
// WITH_STDLIB
// FULL_JDK

// KT-35177: WHEN_ENUM_CAN_BE_NULL_IN_JAVA warning for `when` with java Optional

import java.util.Optional

enum class Color {
    RED, BLUE, GREEN
}

fun getColor() = Color.RED

fun test() {
    val color = getColor()

    when (Optional.ofNullable(color).orElse(Color.BLUE)) {
        Color.RED   -> println("red")
        Color.BLUE  -> println("blue")
        Color.GREEN -> println("green")
    }

    when (Optional.ofNullable(Color.RED).orElse(Color.BLUE)) {
        Color.RED   -> println("red")
        Color.BLUE  -> println("blue")
        Color.GREEN -> println("green")
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, flexibleType, functionDeclaration, localProperty,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
