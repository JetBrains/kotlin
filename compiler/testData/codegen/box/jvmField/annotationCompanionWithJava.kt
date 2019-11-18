// !LANGUAGE: +JvmFieldInInterface +NestedClassesInAnnotations
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Test.java

public class Test {
    public static String publicField() {
        return Foo.z.getS();
    }
}

// FILE: simple.kt


public class Bar(public val s: String)

annotation class Foo {

    companion object {
        @JvmField
        val z = Bar("OK")
    }
}


fun box(): String {
    return Test.publicField()
}
