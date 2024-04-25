// FIR_IDENTICAL
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN
// ISSUE: KT-59138
// SKIP_TXT
// FILE: JavaClass.java
public class JavaClass {
    public static <K> K simpleId(K k) { // fun <K> simpleId(k: K & Any..K?): K & Any..K? =
        return k;
    }
}

// FILE: main.kt

fun takeN(n: Number?): Int = 1

fun bar(n: Number?) {
    fun takeN(n: Number): String = ""

    // in K1 & K2, it is resolved to nullable takeN
    // in K2 with DNN representation ON, it would be resolved to not-nullable and may fail with NPE
    takeN(JavaClass.simpleId(n)).div(1)
    takeN(JavaClass.simpleId(n)).<!UNRESOLVED_REFERENCE!>length<!>
}
