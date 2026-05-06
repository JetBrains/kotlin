// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-48300

// KT-48300: Report warnings when code resolves differently after disabling new inference compatibility mode
fun <T> bar(action: () -> T): T = action()
fun bar(action: java.lang.Runnable) { }

fun foo(): String = ""

fun main() {
    val x = bar() { foo() }
    x.length // OK with compatibility mode, Error with DisableCompatibilityModeForNewInference enabled
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */
