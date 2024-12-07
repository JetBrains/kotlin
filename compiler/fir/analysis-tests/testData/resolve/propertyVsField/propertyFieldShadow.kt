// RUN_PIPELINE_TILL: FRONTEND
// FILE: Test.java

public class Test {
    protected final String text = "ABCD";

    public final String publicPrivateText = "ZYXW";
}

// FILE: test.kt

class Test2 : Test() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>text<!> = "BCDE"

    private val <!PROPERTY_HIDES_JAVA_FIELD!>publicPrivateText<!> = "YXWV"

    fun check() = text // Should be resolved to Test2.text, not to Test.text
}

fun check() = Test2().<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>publicPrivateText<!> // Should be resolved to Test.publicPrivateText (Test2 member is private)

