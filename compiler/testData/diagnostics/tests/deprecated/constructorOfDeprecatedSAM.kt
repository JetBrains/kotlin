// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86428
// LANGUAGE_FEATURE_TOGGLED: ReportDeprecationsOnSamConstructorCalls

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
    <!DEPRECATION_ERROR!>KError<!> { 42 }.foo()
    ::<!DEPRECATION_ERROR!>KError<!>
    <!DEPRECATION!>KWarning<!> { }.bar("!")
    ::<!DEPRECATION!>KWarning<!>
    <!DEPRECATION_ERROR!>KHidden<!> { }.baz()
    ::<!DEPRECATION_ERROR!>KHidden<!>
}

fun jTest() {
    <!DEPRECATION!>JJava<!> { }.bar("!")
    <!DEPRECATION_ERROR!>JError<!> { 42 }.foo()
    <!DEPRECATION!>JWarning<!> { }.bar("!")
    <!DEPRECATION_ERROR!>JHidden<!> { }.baz()
}

fun tTest() {
    <!TYPEALIAS_EXPANSION_DEPRECATION!>TWarning<!> { }.bar("!")
    ::<!TYPEALIAS_EXPANSION_DEPRECATION!>TWarning<!>
    <!DEPRECATION_ERROR!>TError<!> { 42 }.foo()
    ::<!DEPRECATION_ERROR!>TError<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, integerLiteral, interfaceDeclaration, lambdaLiteral,
stringLiteral */
