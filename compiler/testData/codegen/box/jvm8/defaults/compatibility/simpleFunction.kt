// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
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
