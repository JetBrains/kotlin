// !LANGUAGE: -NoDelegationToJavaDefaultInterfaceMembers
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: Base.java

public interface Base {
    String getValue();

    default String test() {
        return getValue();
    }
}

// FILE: main.kt
// JVM_TARGET: 1.8

class OK : Base {
    override fun getValue() = "OK"
}

fun box(): String {
    val z = object : Base by OK() {
        override fun getValue() = "Fail"
    }
    return z.test()
}
