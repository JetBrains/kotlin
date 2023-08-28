// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61370
// MODULE: lib
// FILE: A.java

public abstract class A<T> {
    protected abstract String doIt(T... args);

    public <S extends T> String test(S... args) {
        return doIt(args);
    }
}

// MODULE: main(lib)
// FILE: 1.kt

open class Super
class Sub: Super()

val a: A<Super> =
    object : A<Super>() {
        override fun doIt(vararg parameters: Super): String = "OK"
    }

fun box(): String = a.test<Sub>()
