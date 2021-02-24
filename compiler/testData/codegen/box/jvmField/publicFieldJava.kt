// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: Test.java

public class Test {
    public static String getField()
    {
        return new A().field;
    }
    public static String getCField()
    {
        return A.cfield;
    }
    public static String getObjectField()
    {
        return Object.field;
    }
}

// FILE: publicFieldJava.kt

class A {
    @JvmField public val field = "OK";

    companion object {
        @JvmField public val cfield = "OK";
    }
}

object Object {
    @JvmField public val field = "OK";
}


fun box(): String {
    if (Test.getField() != "OK") return "fail 1: ${Test.getField()}"
    if (Test.getCField() != "OK") return "fail 2: ${Test.getCField()}"
    if (Test.getObjectField() != "OK") return "fail 3: ${Test.getObjectField()}"

    return "OK"

}
