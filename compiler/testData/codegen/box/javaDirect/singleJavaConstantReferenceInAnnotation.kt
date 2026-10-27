// TARGET_BACKEND: JVM
// ISSUE: KT-90786
// IGNORE_BACKEND: JVM
// USE_PSI_JAVA_FACADE: false

// FILE: p/Ann.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann { int value(); }

// FILE: p/Other.java
package p;

public class Other {
    public static final int VALUE = 20;
    public static class Nested { public static final int VALUE = 40; }
}

// FILE: p/Greeter.java
package p;

import static p.Other.VALUE;

public interface Greeter {
    @Ann(Integer.MAX_VALUE) String fromJavaLang();
    @Ann(Other.VALUE) String fromOtherClass();
    @Ann(Other.Nested.VALUE) String fromNestedClass();
    @Ann(VALUE) String fromSingleStaticImport();
}

// FILE: main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate

private fun value(methodName: String): Int =
    Delegator::class.java.getDeclaredMethod(methodName).getAnnotation(Ann::class.java).value

fun box(): String {
    if (value("fromJavaLang") != Int.MAX_VALUE) return "FAIL: java.lang"
    if (value("fromOtherClass") != 20) return "FAIL: other class"
    if (value("fromNestedClass") != 40) return "FAIL: nested class"
    if (value("fromSingleStaticImport") != 20) return "FAIL: static import"
    return "OK"
}
