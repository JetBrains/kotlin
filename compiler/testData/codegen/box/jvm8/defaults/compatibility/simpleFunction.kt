// !JVM_DEFAULT_MODE: compatibility
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: Simple.java

public interface Simple extends KInterface {
    default String test() {
        return KInterface.DefaultImpls.test2(this);
    }
}

// FILE: Foo.java
public class Foo implements Simple {

}

// FILE: main.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface KInterface  {
    @JvmDefault
    fun test2(): String {
        return "OK"
    }
}


fun box(): String {
    val result = Foo().test()
    if (result != "OK") return "fail 1: ${result}"

    return Foo().test2()
}
