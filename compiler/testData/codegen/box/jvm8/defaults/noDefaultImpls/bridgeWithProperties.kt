// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: Simple.java

public interface Simple extends KInterface2 {
    default String test() {
        return getBar();
    }
}

// FILE: Foo.java
public class Foo implements Simple {

}

// FILE: main.kt

interface KInterface<T>  {

    val foo: T

    val bar: T
        get() = foo
}

interface KInterface2 : KInterface<String> {
    override val foo: String
        get() = "OK"
}


fun box(): String {

    val result = Foo().test()
    if (result != "OK") return "fail 1: ${result}"

    return Foo().bar

}
