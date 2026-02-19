// RUN_PIPELINE_TILL: BACKEND
//KT-48160
// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB

import java.io.File

inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    return block(this)
}

fun foo(): Int? {
    try {
        File("123").bufferedWriter().use {
            return 45
        }
    } catch (e: Exception) {
        if (e.message?.startsWith("Remote does not have ") == true) {
            return null
        }

        return null
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
inline, integerLiteral, javaFunction, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall,
stringLiteral, thisExpression, tryExpression, typeConstraint, typeParameter */
