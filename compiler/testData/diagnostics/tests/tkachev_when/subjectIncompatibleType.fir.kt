// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    var x = 1
    when (x) {
        1 -> x++
        <!INCOMPATIBLE_TYPES!>"1"<!> -> x--
        else -> x = 2
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, incrementDecrementExpression, integerLiteral,
localProperty, propertyDeclaration, stringLiteral, whenExpression, whenWithSubject */
