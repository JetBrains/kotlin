// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN

interface A {
    companion object {
        operator fun of(vararg x: Int): A = object : A {}
    }
}

interface B {
    companion object {
        operator fun of(vararg x: Int): B = object : B {}
    }
}

fun <T> expectThroughTV(x: T, y: T) {
}

fun viaSmartcast(x: Any) {
    x as A
    x as B

    expectThroughTV(x, <!UNRESOLVED_REFERENCE!>[42]<!>)
    expectThroughTV(x, <!UNRESOLVED_REFERENCE!>[]<!>)
}

fun viaWhen() {
    expectThroughTV(
        when {
            true -> object : A, B {}
            else -> object : B, A {}
        },
        <!UNRESOLVED_REFERENCE!>["42"]<!>,
    )
}

fun intersectionWithOuterTvInPCLA() {
    class Box<U> {
        fun put(x: U) {
        }
        fun get(): U {
            return null!!
        }
    }

    fun <X> buildBox(block: Box<X>.() -> Unit) { }

    buildBox {
        val x = get()
        x as B
        expectThroughTV([42] /*resolved to A.of() */, x)
        put(A.of())
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox<!> {
        val x = get()
        x as B
        <!CANNOT_INFER_PARAMETER_TYPE!>expectThroughTV<!>(<!UNRESOLVED_REFERENCE!>[42]<!>, x)
        Unit
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, asExpression, checkNotNullCall, classDeclaration, collectionLiteral,
companionObject, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration, intersectionType,
lambdaLiteral, localClass, localFunction, localProperty, nullableType, objectDeclaration, operator, propertyDeclaration,
smartcast, stringLiteral, typeParameter, typeWithExtension, vararg, whenExpression */
