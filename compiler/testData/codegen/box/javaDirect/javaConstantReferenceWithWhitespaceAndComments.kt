// TARGET_BACKEND: JVM
// ISSUE: KT-90786

// FILE: p/NumberAnn.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NumberAnn { int value(); }

// FILE: p/EnumAnn.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnumAnn { E value(); }

// FILE: p/Other.java
package p;

public class Other {
    public static final int X = 20;
    public static final int SPACED = Other /* before dot */ . /* after dot */ X;
}

// FILE: p/E.java
package p;

public enum E { VALUE }

// FILE: p/Greeter.java
package p;

public interface Greeter {
    @NumberAnn(Other . X) String spaced();
    @NumberAnn(Other./* comment */X) String commented();
    @NumberAnn(Other . X + 1) String expression();
    @NumberAnn(Other . SPACED) String fieldInitializer();
    @EnumAnn(E /* comment */ . VALUE) String enumEntry();
}

// FILE: main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate

private fun numberValue(methodName: String): Int =
    Delegator::class.java.getDeclaredMethod(methodName).getAnnotation(NumberAnn::class.java).value

fun box(): String {
    if (numberValue("spaced") != 20) return "FAIL: spaced reference"
    if (numberValue("commented") != 20) return "FAIL: commented reference"
    if (numberValue("expression") != 21) return "FAIL: expression reference"
    if (numberValue("fieldInitializer") != 20) return "FAIL: field initializer reference"
    if (Delegator::class.java.getDeclaredMethod("enumEntry").getAnnotation(EnumAnn::class.java).value != E.VALUE) {
        return "FAIL: enum entry"
    }
    return "OK"
}