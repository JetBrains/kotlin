// FILE: Extensions.java
package test;

class GenericArray {
    {
        ExtensionsKt.noParam("");

        ExtensionsKt.param("", "");
    }
}

// FILE: Extensions.kt
package test

fun String.noParam() {
}

fun String.param(p: String) {
}
