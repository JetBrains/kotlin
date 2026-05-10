// RUN_PIPELINE_TILL: BACKEND
interface Box<T>

public fun <T> foo(nextFunction: (T) -> T): Box<T> = null!!

fun leaves(value: String, forward: Boolean): Box<String> {
    if (forward) {
        return foo { "" }
    } else {
        return foo { "" }
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, functionDeclaration, functionalType, ifExpression, interfaceDeclaration,
lambdaLiteral, nullableType, stringLiteral, typeParameter */
