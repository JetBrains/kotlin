// FILE: JavaClass.java
public class JavaClass extends KotlinClass {
    @Override
    public int getSomething() { return 1; }
    @Override
    public void setSomething(int value) {}
}

// FILE: main.kt
open class KotlinClass {
    open var something: Int = 1
}

fun JavaClass.foo(javaClass: JavaClass) {
    print(javaClass.<caret_1>something)
    javaClass.<caret_2>something = 1
    javaClass.<caret_3>something += 1
    javaClass.<caret_4>something++
    --javaClass.<caret_5>something
    <caret_6>something++
}

fun bar(kotlinClass: KotlinClass) {
    if (kotlinClass is JavaClass) {
        print(kotlinClass.<caret_7>something)
        kotlinClass.<caret_8>something = 1
        kotlinClass.<caret_9>something += 1
        kotlinClass.<caret_10>something++
        --kotlinClass.<caret_11>something
    }
}