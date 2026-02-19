// IGNORE_STABILITY_K2: candidates

// FILE: main.kt
class KotlinClass : JavaClass() {
    override fun getSomething(): Int {
        return 2
    }

    override fun setSomething(value: Int) {}
}

fun KotlinClass.foo(kotlinClass: KotlinClass) {
    print(kotlinClass.<caret_1>something)
    kotlinClass.<caret_2>something = 1
    kotlinClass.<caret_3>something += 1
    kotlinClass.<caret_4>something++
    --kotlinClass.<caret_5>something
    <caret_6>something++
}

fun bar(javaClass: JavaClass) {
    if (javaClass is KotlinClass) {
        print(javaClass.<caret_7>something)
        javaClass.<caret_8>something = 1
        javaClass.<caret_9>something += 1
        javaClass.<caret_10>something++
        --javaClass.<caret_11>something
    }
}

// FILE: JavaClass.java
public class JavaClass {
    public int getSomething() { return 1; }
    public void setSomething(int value) {}
}
