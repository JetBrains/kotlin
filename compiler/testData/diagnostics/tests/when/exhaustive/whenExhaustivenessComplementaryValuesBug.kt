// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87189

sealed interface A
sealed interface B : A

data object X : B
data object Y : B, A

fun Any.test(): String =
    when (this) {
        is B -> {
             <!NO_ELSE_IN_WHEN!>when<!> (this) {
                is X -> "is X"
            }
        }
        else -> "is not B"
    }

/* GENERATED_FIR_TAGS: data, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, isExpression,
objectDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
