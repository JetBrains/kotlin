// FILE: main.kt

fun foo(j: MyJavaClass) {
    j.bar()
}

abstract class MyClass {
    open fun bar(y: String? = null) {}
}

// FILE: MyJavaClass.java

public class MyJavaClass extends MyClass {
    public void bar(String y) {}
}
