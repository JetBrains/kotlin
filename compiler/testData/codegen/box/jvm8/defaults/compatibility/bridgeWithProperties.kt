// !JVM_DEFAULT_MODE: compatibility
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: Simple.java

public interface Simple extends KInterface2 {
    default String test() {
        return KInterface2.DefaultImpls.getBar(this);
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

    @JvmDefault
    val bar: T
        get() = foo
}

interface KInterface2 : KInterface<String> {
    @JvmDefault
    override val foo: String
        get() = "OK"
}


fun box(): String {

    val result = Foo().test()
    if (result != "OK") return "fail 1: ${result}"

    return Foo().bar

}
