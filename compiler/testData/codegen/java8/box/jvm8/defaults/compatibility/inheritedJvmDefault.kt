// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: compatibility
// FILE: Simple.java

public interface Simple extends KInterface2 {
    default String test() {
        return KInterface2.DefaultImpls.test2(this);
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

interface KInterface2 : KInterface {

}


fun box(): String {
    val result = Foo().test()
    if (result != "OK") return "fail 1: ${result}"

    return Foo().test2()
}