// TARGET_BACKEND: JVM_IR
// ISSUE: KT-78589
// WITH_STDLIB

// FILE: TestJava.java
public class TestJava {
    String javaTestString;
}

// FILE: TestKotlin.kt
class TestKotlin(testJava: TestJava) {
    var kotlinTestString: String? by testJava::javaTestString
}

fun box(): String {
    val testJava = TestJava()
    val testKotlin = TestKotlin(testJava)

    require(testKotlin.kotlinTestString == null)
    testKotlin.kotlinTestString = "test"
    require(testKotlin.kotlinTestString == "test") { testKotlin.kotlinTestString.toString() }

    return "OK"
}
