// RUN_PIPELINE_TILL: FRONTEND
package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> T.use(block: (t: T)-> R) : R {
    return block(this)
}

fun test() {
    C().use {
        it.close()
        <!UNRESOLVED_REFERENCE!>x<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, inline,
interfaceDeclaration, lambdaLiteral, nullableType, thisExpression, typeConstraint, typeParameter */
