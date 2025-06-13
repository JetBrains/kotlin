// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    var f: Int = if (true) <!TYPE_MISMATCH!>{ x: Long ->  }<!> else <!TYPE_MISMATCH!>{ x: Long ->  }<!>
}

class A {
    var x: Int
        get(): Int = if (true) <!TYPE_MISMATCH!>{ {42} }<!> else <!TYPE_MISMATCH!>{ {24} }<!>
        set(i: Int) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, ifExpression, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, setter */
