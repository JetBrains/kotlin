// RUN_PIPELINE_TILL: CODEGEN
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
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>KError<!> { 42 }.foo()
    ::<!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>KError<!>
    <!DEPRECATION!>KWarning<!> { }.bar("!")
    ::<!DEPRECATION!>KWarning<!>
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>KHidden<!> { }.baz()
    ::<!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>KHidden<!>
}

fun jTest() {
    <!DEPRECATION!>JJava<!> { }.bar("!")
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>JError<!> { 42 }.foo()
    <!DEPRECATION!>JWarning<!> { }.bar("!")
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>JHidden<!> { }.baz()
}

fun tTest() {
    <!TYPEALIAS_EXPANSION_DEPRECATION!>TWarning<!> { }.bar("!")
    ::<!TYPEALIAS_EXPANSION_DEPRECATION!>TWarning<!>
    <!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>TError<!> { 42 }.foo()
    ::<!DEPRECATION_ERROR_MIGRATION_PERIOD_WARNING!>TError<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, integerLiteral, interfaceDeclaration, lambdaLiteral,
stringLiteral */
