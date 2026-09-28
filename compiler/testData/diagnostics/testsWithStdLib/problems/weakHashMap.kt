// RUN_PIPELINE_TILL: CODEGEN
// FULL_JDK

import java.util.*

val someMap = WeakHashMap<Any?, Any?>()

fun foo() {
    someMap[""]
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, nullableType, propertyDeclaration, stringLiteral */
