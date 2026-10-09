// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN
// LANGUAGE: +CompanionBlocks

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

    expectThroughTV(x, [42])
    expectThroughTV(x, <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
}

fun viaWhen() {
    expectThroughTV(
        when {
            true -> object : A, B {}
            else -> object : B, A {}
        },
        ["42"],
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
        expectThroughTV([42] /*resolved to List.of() */, x)
        put(A.of())
    }

    buildBox {
        val x = get()
        x as B
        expectThroughTV([42], x)
        Unit
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, asExpression, checkNotNullCall, classDeclaration, collectionLiteral,
companionObject, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration, intersectionType,
lambdaLiteral, localClass, localFunction, localProperty, nullableType, objectDeclaration, operator, propertyDeclaration,
smartcast, stringLiteral, typeParameter, typeWithExtension, vararg, whenExpression */
