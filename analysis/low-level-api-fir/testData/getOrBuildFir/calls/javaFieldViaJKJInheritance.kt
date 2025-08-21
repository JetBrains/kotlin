// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: MyClass.java
public class MyClass {
    int myField = 1000;
}

// FILE: MyClassImpl.java
public class MyClassImpl extends MyClassEx {
}

// FILE: main.kt
open class MyClassEx : MyClass()

fun main(j: MyClassImpl) {
    j.<expr>myField</expr>
}
