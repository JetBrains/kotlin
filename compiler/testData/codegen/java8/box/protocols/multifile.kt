// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

// FILE: Proto.kt
protocol interface Proto {
    fun OK(): String
}

class A {
    fun OK(): String = "O"
}

class B {
    fun OK(): String = "K"
}

// FILE: First.kt
fun x(arg: Proto): String = arg.OK()

// FILE: Second.kt
fun y(arg: Proto): String = arg.OK()

// FILE: Test.kt
fun box(): String = "${x(A())}${y(B())}"
