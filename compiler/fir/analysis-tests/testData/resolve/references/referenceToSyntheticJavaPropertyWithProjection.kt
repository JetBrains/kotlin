// ISSUE: KT-56243
// !LANGUAGE: +ReferencesToSyntheticJavaProperties

// FILE: JavaInv.java
public class JavaInv<T> {
    public String getStringVal() { return "OK"; }
}

// FILE: main.kt

class KotlinInv<T> {
    val stringVal: String = ""
}

fun test() {
    JavaInv<out Number>::stringVal
    KotlinInv<out Number>::stringVal
}
