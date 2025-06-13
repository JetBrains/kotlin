// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class Cell {
    operator fun get(s: Int) = 1
}

fun box(): String {
    val c = Cell()
    (c<!NO_SET_METHOD!>[0]<!>)++
    return "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, incrementDecrementExpression, integerLiteral,
localProperty, operator, propertyDeclaration, stringLiteral */
