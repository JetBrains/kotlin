// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: Simple.java

public interface Simple extends KInterface2 {
    default String test() {
        return KInterface2.DefaultImpls.getBar(this);
    }
}

// FILE: Foo.java
public class Foo implements Simple {
    public String getBar() {
        return "fail";
    }
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
    return Foo().test()
}
