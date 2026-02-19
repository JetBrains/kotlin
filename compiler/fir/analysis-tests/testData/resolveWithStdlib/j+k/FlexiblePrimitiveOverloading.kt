// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
import java.lang.Integer.getInteger

fun foo() {
    getInteger("text", 239)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, stringLiteral */
