// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun ff(): Int {
    var i = 1
    for (<!NAME_SHADOWING!>i<!> in 0..10) {
    }

    for ((<!NAME_SHADOWING!>i<!>, j) in listOf(Pair(1,2))) {
        val <!NAME_SHADOWING!>i<!> = i
    }

    return i
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, integerLiteral, localProperty, propertyDeclaration, rangeExpression */
