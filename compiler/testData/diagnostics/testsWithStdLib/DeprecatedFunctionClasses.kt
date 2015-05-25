// FILE: J.java

import kotlin.ExtensionFunction0;
import kotlin.ExtensionFunction1;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;

public class J {
    public static void f1(Function0<Unit> f) {
        f.invoke();
    }

    public static void f2(Function1<String, String> f) {
        f.invoke("");
    }

    public static void ef1(ExtensionFunction1<Integer, Integer, Integer> ef) {
        ef.invoke(42, -42);
    }

    public static ExtensionFunction0<String, Unit> ef2() {
        return null;
    }
}

// FILE: K.kt

fun foo() = J.<!JAVA_METHOD_USES_DEPRECATED_FUNCTION_CLASS!>f1 { }<!>

fun bar() = J.<!JAVA_METHOD_USES_DEPRECATED_FUNCTION_CLASS!>f2 { it }<!>

fun baz() = J.<!JAVA_METHOD_USES_DEPRECATED_FUNCTION_CLASS!>ef1 <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!> <!NO_THIS!>this<!> }<!>

fun quux() = J.<!JAVA_METHOD_USES_DEPRECATED_FUNCTION_CLASS!>ef2()<!>
