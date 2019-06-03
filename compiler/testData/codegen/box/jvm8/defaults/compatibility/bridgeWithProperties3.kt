// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM
// FILE: Simple.java

public interface Simple extends KInterface3 {
    default String test() {
        return KInterface3.DefaultImpls.getBar(this);
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

interface KInterface3 : KInterface2 {

}


fun box(): String {
    return Foo().test()
}
