// TARGET_BACKEND: JVM
// FILE: Base.java

public interface Base {
    String getValue();

    default String test() {
        return getValue();
    }
}

// FILE: Base2.java
public interface Base2 extends Base {

    default String test() {
        return "O"  + getValue();
    }
}


// FILE: main.kt

interface KBase : Base

interface Derived : KBase, Base2

class Fail : Derived {
    override fun getValue() = "Fail"
}

fun box(): String {
    val z = object : Derived by Fail() {
        override fun getValue() = "K"
    }
    return z.test()
}
