// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object B<!> {
    val y = 5
    init {
        println("A = " + A)
        println(<!POTENTIALLY_UNINITIALIZED_ACCESS!>A.y<!>)
    }
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object A<!> {
    val x = B.y
    val y = "test"
    init {
        println("B = " + B)
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, init, integerLiteral, objectDeclaration, propertyDeclaration, stringLiteral */
