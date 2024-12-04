// FILE: main.kt
fun JavaClass.foo(javaClass: JavaClass) {
    print(javaClass.<caret>something)
    javaClass.<caret>something = 1
    javaClass.<caret>something += 1
    javaClass.<caret>something++
    --javaClass.<caret>something

    <caret>something++
    (<caret>something)++
    (<caret>something) = 1
    (javaClass.<caret>something) = 1
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
    public void setSomething(int value) {}
}