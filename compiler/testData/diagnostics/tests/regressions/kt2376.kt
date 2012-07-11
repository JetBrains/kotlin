// FILE: main.kt
//KT-2376 java.lang.Number should be visible in Kotlin as jet.Number
fun main(args: Array<String>) {
    Test().number(5.toInt())
}

// FILE: Test.java
public class Test {
    void number(Number n){}
}
