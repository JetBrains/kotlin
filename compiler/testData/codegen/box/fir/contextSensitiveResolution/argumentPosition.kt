// ISSUE: KT-75315
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: JavaClass.java
public class JavaClass {
    public static JavaClass INSTANCE = new JavaClass();

    public static String foo4(JavaClass j) {
        return j.toString();
    }

    public String toString() {
        return "OK";
    }
}

// FILE: main.kt
enum class MyEnum {
    OK
}

fun foo1(m: MyEnum): String = m.name

sealed class MySealed {
    data object Ok : MySealed()
}

fun foo2(m: MySealed): String = m.toString()

class MyClass {
    companion object {
        val INSTANCE = MyClass()
    }

    override fun toString() = "OK"
}

fun foo3(m: MyClass): String = m.toString()

fun box(): String {
    if (foo1(OK) != "OK") return "fail 1"
    if (foo2(Ok) != "Ok") return "fail 2"
    if (foo3(INSTANCE) != "OK") return "fail 3"
    if (JavaClass.foo4(INSTANCE) != "OK") return "fail 4"
    return "OK"
}
