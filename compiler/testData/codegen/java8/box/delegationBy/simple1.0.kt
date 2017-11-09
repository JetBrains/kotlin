// LANGUAGE_VERSION: 1.0
// FILE: Base.java

public interface Base {
    String getValue();

    default String test() {
        return getValue();
    }
}

// FILE: main.kt
class OK : Base {
    override fun getValue() = "OK"
}

fun box(): String {
    val z = object : Base by OK() {
        override fun getValue() = "Fail"
    }
    return z.test()
}
