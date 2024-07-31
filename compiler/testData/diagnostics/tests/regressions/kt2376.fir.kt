// FILE: main.kt
//KT-2376 java.lang.Number should be visible in Kotlin as kotlin.Number
fun main() {
    Test().number(5.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>)
}

// FILE: Test.java
public class Test {
    void number(Number n){}
}
