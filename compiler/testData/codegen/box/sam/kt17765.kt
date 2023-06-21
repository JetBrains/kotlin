// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +EliminateAmbiguitiesOnInheritedSamInterfaces

// FILE: Test.java
public class Test {
    interface MyRunnable extends Runnable {}

    public static void foo(MyRunnable r) {}
    public static void foo(Runnable r) {}
}

// FILE: 1.kt
fun main(args: Array<String>) {
    Test.foo {  }
}

fun box(): String {
    main(arrayOf("", ""))
    return "OK"
}