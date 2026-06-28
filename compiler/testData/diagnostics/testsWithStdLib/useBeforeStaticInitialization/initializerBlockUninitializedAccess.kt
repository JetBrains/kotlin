// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object A<!> {
    val x = 1
    init {
        println(<!ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY!>B.y<!>)
    }
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object B<!> {
    val x = A.x
    val y = "foo"
}

/* GENERATED_FIR_TAGS: init, integerLiteral, objectDeclaration, propertyDeclaration, stringLiteral */
