// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: LibJava1.java
public class LibJava1 {
    public String method() {
        return "";
    }
}

// FILE: LibJava2.java
import kotlin.MustUseReturnValues;

@MustUseReturnValues
public class LibJava2 {
    public String method() {
        return "";
    }
}

// FILE: Lib.kt
class LibKotlin {
    fun method() = ""
}

fun foo() {
    LibJava1().method()
    LibJava2().<!RETURN_VALUE_NOT_USED!>method<!>()
    LibKotlin().<!RETURN_VALUE_NOT_USED!>method<!>()
}

// MODULE: main(lib1)

// FILE: App.kt
fun bar() {
    LibJava1().method()
    LibJava2().<!RETURN_VALUE_NOT_USED!>method<!>()
    LibKotlin().<!RETURN_VALUE_NOT_USED!>method<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral */
