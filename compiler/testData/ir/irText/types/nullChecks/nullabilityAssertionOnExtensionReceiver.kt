// FILE: nullabilityAssertionOnExtensionReceiver.kt

fun String.extension() {}

class C {
    fun String.memberExtension() {}
}

fun testExt() {
    J.s().extension()
}

fun C.testMemberExt() {
    J.s().memberExtension()
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}
