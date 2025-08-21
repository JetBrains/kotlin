// RUN_PIPELINE_TILL: FRONTEND
package a

interface Closeable {
    fun close() {}
}

class C : Closeable

public inline fun <T: Closeable, R> use(t: T, block: T.(T)-> R) : R {
    return t.block(t)
}

fun test() {
    use(C()) {
        this.close()
        it.close()
        <!UNRESOLVED_REFERENCE!>xx<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, inline, interfaceDeclaration,
lambdaLiteral, nullableType, thisExpression, typeConstraint, typeParameter, typeWithExtension */
