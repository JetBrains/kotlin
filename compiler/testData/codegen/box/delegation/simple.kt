// TARGET_BACKEND: JVM
// FILE: Base.java

public interface Base {
    String getValue();

    default String test() {
        return getValue();
    }
}

// FILE: main.kt
class Fail : Base {
    override fun getValue() = "Fail"
}

fun box(): String {
    val z = object : Base by Fail() {
        override fun getValue() = "OK"
    }
    return z.test()
}
