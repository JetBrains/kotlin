// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: MyInterface.java
public interface MyInterface {
    int MY_STATIC_FIELD = 1000;
}

// FILE: MyInterfaceImpl.java
public class MyInterfaceImpl implements MyInterfaceEx {
}

// FILE: main.kt
interface MyInterfaceEx : MyInterface

fun main() {
    MyInterfaceImpl.<expr>MY_STATIC_FIELD</expr>
}
