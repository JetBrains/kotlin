// !JVM_DEFAULT_MODE: all
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: Simple.java

public interface Simple extends KInterface3 {
    default String test() {
        return getBar();
    }
}

// FILE: Foo.java
public class Foo implements Simple {

}

// FILE: main.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface KInterface<T>  {

    val foo: T

    val bar: T
        get() = foo
}

interface KInterface2 : KInterface<String> {
    override val foo: String
        get() = "OK"
}

interface KInterface3 : KInterface2 {

}


fun box(): String {
    return Foo().test()
}
