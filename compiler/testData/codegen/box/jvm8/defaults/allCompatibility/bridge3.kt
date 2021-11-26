// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: Simple.java

public interface Simple extends KInterface3 {
    default String test() {
        return KInterface3.DefaultImpls.test2(this, "OK");
    }
}

// FILE: Foo.java
public class Foo implements Simple {
    public String test2(String p) {
        return "fail";
    }
}

// FILE: main.kt

interface KInterface<T>  {
    fun test2(p: T): T {
        return p
    }
}

interface KInterface2 : KInterface<String> {

}

interface KInterface3 : KInterface2 {

}


fun box(): String {
    return Foo().test()
}
