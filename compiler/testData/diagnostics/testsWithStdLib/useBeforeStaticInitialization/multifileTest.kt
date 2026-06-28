// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB

// FILE: test1.kt
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object B<!> {
    val x = run {
        println("smth")
        z
    }
    val y = run {
        println("smth")
        test
    }
}

val test = "foo"

// FILE: test2.kt
val z = 1

<!POSSIBLY_UNINITIALIZED_PROPERTY!>val w = <!ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY!>B.y<!><!>

/* GENERATED_FIR_TAGS: integerLiteral, lambdaLiteral, objectDeclaration, propertyDeclaration, stringLiteral */
