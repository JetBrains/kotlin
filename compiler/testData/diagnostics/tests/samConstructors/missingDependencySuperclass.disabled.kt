// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ForbidSamConstructorCallsWithMissingDependencySupertype
// ISSUE: KT-81075

// MODULE: dep
// FILE: dep/Callback.java
package dep;

public interface Callback {
    void foo();
}

// MODULE: lib(dep)
// FILE: lib/ThemeCallback.java
package lib;

import dep.Callback;

public interface ThemeCallback extends Callback {
    void callback(boolean isDark);
}

// FILE: lib/Test.java
package lib;

import dep.Callback;

public class Test {
    public static void test(Object o) {
        ((Callback) o).foo();
    }

    public static void take(ThemeCallback c) {
        test(c);
    }
}

// MODULE: app(lib)
// FILE: main.kt
import lib.ThemeCallback
import lib.Test

class FrontendThemeManager {
    val nativeThemeCallback = <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>ThemeCallback<!> {}
    val nativeThemeCallbackConversion: ThemeCallback = <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>{}<!>
}

fun box(): String {
    // Would result in "AbstractMethodError: Receiver class FrontendThemeManager$$Lambda...
    // does not define or inherit an implementation of the resolved method
    // 'abstract void foo()' of interface dep.Callback."
    Test.test(FrontendThemeManager().nativeThemeCallback)
    Test.test(FrontendThemeManager().nativeThemeCallbackConversion)
    Test.take <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>{}<!>
    return "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, lambdaLiteral, propertyDeclaration,
samConversion, stringLiteral */
