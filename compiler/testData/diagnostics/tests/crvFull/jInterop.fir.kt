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
import kotlin.MustUseReturnValue;

@MustUseReturnValue
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
    <!RETURN_VALUE_NOT_USED!>LibJava2().method()<!>
    <!RETURN_VALUE_NOT_USED!>LibKotlin().method()<!>
}

// MODULE: main(lib1)

// FILE: App.kt
fun bar() {
    LibJava1().method()
    <!RETURN_VALUE_NOT_USED!>LibJava2().method()<!>
    <!RETURN_VALUE_NOT_USED!>LibKotlin().method()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral */
