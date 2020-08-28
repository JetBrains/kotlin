// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// FILE: Simple.java

public interface Simple extends KInterface2 {
    default String test() {
        return KInterface2.super.test2("OK");
    }
}

// FILE: Foo.java
public class Foo implements Simple {

}

// FILE: main.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface KInterface<T>  {
    fun test2(p: T): T {
        return p
    }
}

interface KInterface2 : KInterface<String> {

}


fun box(): String {

    val result = Foo().test()
    if (result != "OK") return "fail 1: ${result}"

    return Foo().test2("OK")
}
