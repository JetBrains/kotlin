// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExplicitBackingFields +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect fun calculateFlag(): Boolean

object O {
    val f: CharSequence
        field: String = when (val flag = calculateFlag()) {
            true -> "OK"
            false -> "Not OK"
        }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun calculateFlag() = true

fun bar(): String = <!RETURN_TYPE_MISMATCH!>O.f<!>

/* GENERATED_FIR_TAGS: actual, equalityExpression, expect, explicitBackingField, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
