// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-86428

// FILE: JJava.java
@Deprecated
public interface JJava {
    void bar(String s);
}

// FILE: JError.java
import kotlin.DeprecationLevel;

@kotlin.Deprecated(message = "", level = DeprecationLevel.ERROR)
public interface JError {
    int foo();
}

// FILE: JWarning.java
import kotlin.DeprecationLevel;

@kotlin.Deprecated(message = "", level = DeprecationLevel.WARNING)
public interface JWarning {
    void bar(String s);
}

// FILE: JHidden.java
import kotlin.DeprecationLevel;

@kotlin.Deprecated(message = "", level = DeprecationLevel.HIDDEN)
public interface JHidden {
    void baz();
}

// FILE: main.kt
@Deprecated("", level = DeprecationLevel.ERROR)
fun interface KError {
    fun foo(): Int
}

@Deprecated("", level = DeprecationLevel.WARNING)
fun interface KWarning {
    fun bar(s: String)
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
fun interface KHidden {
    fun baz()
}

typealias TWarning = <!DEPRECATION!>KWarning<!>

fun interface KNo {
    fun foo(): Int
}

@Deprecated("", level = DeprecationLevel.ERROR)
typealias TError = KNo

fun kTest() {
    KError { 42 }.foo()
    ::KError
    KWarning { }.bar("!")
    ::KWarning
    KHidden { }.baz()
    ::KHidden
}

fun jTest() {
    JJava { }.bar("!")
    JError { 42 }.foo()
    JWarning { }.bar("!")
    JHidden { }.baz()
}

fun tTest() {
    TWarning { }.bar("!")
    ::TWarning
    TError { 42 }.foo()
    ::TError
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, integerLiteral, interfaceDeclaration, lambdaLiteral,
stringLiteral */
