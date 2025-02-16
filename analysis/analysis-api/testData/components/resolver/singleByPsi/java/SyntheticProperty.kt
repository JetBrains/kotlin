// IGNORE_STABILITY_K2: candidates

// FILE: main.kt
fun JavaClass.foo(javaClass: JavaClass) {
    print(javaClass.<caret_1>something)
    javaClass.<caret_2>something = 1
    javaClass.<caret_3>something += 1
    javaClass.<caret_4>something++
    --javaClass.<caret_5>something

    <caret_6>something++
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
    public void setSomething(int value) {}
}
