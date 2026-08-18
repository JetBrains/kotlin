// FILE: Extensions.java
package test;

class GenericArray {
    {
        ExtensionsKt.getVal_("");

        ExtensionsKt.getVar_("");
        ExtensionsKt.setVar_("", "");
    }
}

// FILE: Extensions.kt
package test

val String.val_: String
    get() = ""

var String.var_: String
    get() = ""
    set(value) {
    }
