// FILE: JavaClass.java

public class JavaClass {
    public String foo() {
        return "";
    }
}

// FILE: main.kt
fun test(jc: JavaClass) {
    jc.fo<caret>o()
}
