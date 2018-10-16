// TARGET_BACKEND: JVM
// FILE: Base.java

public interface Base {
    String getValue();

    default String test()
    {
        return getValue();
    }
}

// FILE: Base2.java
public interface Base2 extends Base {

}


// FILE: main.kt

interface KBase : Base {
    override fun test() = "O" + getValue()
}

interface Derived : KBase, Base2

class K : Derived {
    override fun getValue() = "K"
}

fun box(): String {
    val z = object : Derived by K() {
        override fun getValue() = "Fail"
    }
    return z.test()
}
