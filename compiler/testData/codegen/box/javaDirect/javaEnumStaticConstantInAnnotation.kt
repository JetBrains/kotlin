// TARGET_BACKEND: JVM
// ISSUE: KT-90786

// FILE: p/Ann.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann { int value(); }

// FILE: p/EnumAnn.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import q.E;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnumAnn { E value(); }

// FILE: q/E.java
package q;

public enum E {
    A, B;
    public static final int X = 5;
}

// FILE: p/Greeter.java
package p;

import static q.E.*;

public interface Greeter {
    @Ann(q.E.X) String enumClassConstant();
    @Ann(JE.Y) String sameFileEnumClassConstant();
    @Ann(KEnum.NUMBER) String kotlinEnumCompanionConstant();
    @EnumAnn(q.E.A) String qualifiedEnumEntry();
    @EnumAnn(A) String enumEntryViaStaticStarImport();
}

// FILE: main.kt
package p

import q.E

enum class JE {
    VALUE;

    companion object {
        const val Y = 6
    }
}

enum class KEnum {
    VALUE;

    companion object {
        const val NUMBER = 7
    }
}

class Delegator(delegate: Greeter) : Greeter by delegate

private fun intValue(methodName: String): Int =
    Delegator::class.java.getDeclaredMethod(methodName).getAnnotation(Ann::class.java).value

private fun enumValue(methodName: String): E =
    Delegator::class.java.getDeclaredMethod(methodName).getAnnotation(EnumAnn::class.java).value

fun box(): String {
    if (intValue("enumClassConstant") != 5) return "FAIL: Java enum field"
    if (intValue("sameFileEnumClassConstant") != 6) return "FAIL: same-file enum field"
    if (intValue("kotlinEnumCompanionConstant") != 7) return "FAIL: Kotlin enum companion constant"
    if (enumValue("qualifiedEnumEntry") != E.A) return "FAIL: qualified enum entry"
    if (enumValue("enumEntryViaStaticStarImport") != E.A) return "FAIL: static-star enum entry"
    return "OK"
}