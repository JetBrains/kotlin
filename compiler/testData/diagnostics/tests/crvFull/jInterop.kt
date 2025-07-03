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
    LibJava2().method()
    LibKotlin().method()
}

// MODULE: main(lib1)

// FILE: App.kt
fun bar() {
    LibJava1().method()
    LibJava2().method()
    LibKotlin().method()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral */
