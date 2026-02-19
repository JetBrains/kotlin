// RUN_PIPELINE_TILL: FRONTEND
class StringBuilder {
    fun append(s: String) {}
}

fun buildString(init: StringBuilder.() -> Unit): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

interface Template<in X>

class KDocTemplate : Template<StringBuilder> {
    fun definition(content: StringBuilder.() -> Unit) {}
}

fun <U, T : Template<U>> U.insert(template: T, build: T.() -> Unit) {}

fun test(ordinal: Int) {
    buildString {
        insert(KDocTemplate()) {
            definition {
                ordinal<!UNNECESSARY_SAFE_CALL!>?.<!>let {}
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, in,
interfaceDeclaration, lambdaLiteral, nullableType, safeCall, typeConstraint, typeParameter, typeWithExtension */
