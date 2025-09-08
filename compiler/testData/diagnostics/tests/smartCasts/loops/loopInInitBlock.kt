// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77563

class Test() {
    init {
        var num: Int? = 3
        val numNotNull = num!!
        for (x in 1..2) {
            val num2 = num
            num<!UNSAFE_CALL!>.<!>inc()
            num = null
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, forLoop, init, integerLiteral, localProperty,
nullableType, primaryConstructor, propertyDeclaration, rangeExpression */
