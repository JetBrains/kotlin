// TARGET_BACKEND: JVM
// FILE: Base.java

public interface Base extends KBase {
    String getValue();

    default String test() {
        return getValue();
    }
}

// FILE: main.kt
interface KBase {
    fun getValue(): String

    fun test(): String
}

class Fail : Base {
    override fun getValue() = "Fail"
}

fun box(): String {
    val z1 = object : KBase by Fail() {
        override fun getValue() = "OK"
    }
    if (z1.test() != "Fail") return "fail 1"

    val z2 = object : Base by Fail() {
        override fun getValue() = "OK"
    }
    return z2.test()
}
