// TARGET_BACKEND: JVM_IR
// ISSUE: KT-78589
// WITH_STDLIB

// FILE: TestJava.java
public class TestJava {
    String testString;
}

// FILE: TestKotlin.kt

class TestKotlin(testJava: TestJava) {
    val testString: String? by testJava::testString
}

fun box(): String {
    val testJava = TestJava()
    testJava.testString = "test"

    val testKotlin = TestKotlin(testJava)
    require(testKotlin.testString == "test") { testKotlin.testString.toString() }
    return "OK"
}
