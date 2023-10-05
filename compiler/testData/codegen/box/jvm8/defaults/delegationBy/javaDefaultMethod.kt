// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// IGNORE_BACKEND_K2: ANY
// Ignore reason: KT-62334

// FILE: javaDefaultMethod.kt

class JImpl : J {
    override fun getO() = "fail"
    override fun getK() = "K"
}

class Test : J by JImpl()

fun box() =
    Test().getO() + Test().getK()

// FILE: J.java
public interface J {
    default String getO() {
        return "O";
    }

    String getK();
}
