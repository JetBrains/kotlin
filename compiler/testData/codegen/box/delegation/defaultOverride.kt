// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM

// FILE: Base.java

public interface Base {
    String getValue();

    default String test() {
        return getValue();
    }
}

// FILE: main.kt

public interface BaseKotlin : Base {
    override fun getValue() = "OK"

    override fun test(): String {
        return getValue();
    }
}

class OK : BaseKotlin {
    override fun getValue() = "OK"
}

fun box(): String {
    val ok = object : BaseKotlin by OK() {
        override fun getValue() = "Fail"
    }
    return ok.test()
}
