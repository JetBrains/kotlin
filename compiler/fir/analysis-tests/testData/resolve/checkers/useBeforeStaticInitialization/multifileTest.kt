// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB

// FILE: test1.kt
private val init = run {
    println("Test1Kt.<clinit>")
}

object B {
    init {
        println("B.<clinit>")
    }
    val x = run {
        println("B.x.<init>")
        z
    }
    val y = run {
        println("B.y.<init>")
        test
    }
}

val test = "foo"

// FILE: test2.kt
private val init = run {
    println("Test2Kt.<clinit>")
}

val z = 1

<!UNINITIALIZED_PROPERTY!>val w = <!UNINITIALIZED_ACCESS!>B.y<!><!>
