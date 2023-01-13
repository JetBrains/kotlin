// FILE: Test.java

public class Test {
    protected final String text = "ABCD";

    public final String publicPrivateText = "ZYXW";
}

// FILE: test.kt

class Test2 : Test() {
    private val text = "BCDE"

    private val publicPrivateText = "YXWV"

    fun check() = text // Should be resolved to Test2.text, not to Test.text
}

fun check() = Test2().publicPrivateText // Should be resolved to Test.publicPrivateText (Test2 member is private)

