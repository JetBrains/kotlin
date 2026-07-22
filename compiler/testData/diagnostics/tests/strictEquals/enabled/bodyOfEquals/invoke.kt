// RUN_PIPELINE_TILL: BACKEND

class Invoker {
    operator fun invoke() = 42
    override fun equals(@EqualityBound(Invoker::class) other: Any?): Boolean {
        return this() == other()
    }
}

class FunctionChild : Function0<Int> {
    override fun invoke(): Int = 42

    override fun equals(@EqualityBound(Function0::class) other: Any?): Boolean {
        return this() == other()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, integerLiteral,
nullableType, operator, override, smartcast, starProjection, thisExpression */
