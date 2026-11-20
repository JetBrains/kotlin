// TARGET_BACKEND: JVM
// ISSUE: KT-90787

// MODULE: lib
// FILE: q/Lib.java
package q;

public interface Lib { int LIB = 42; }

// MODULE: main(lib)
// FILE: p/Ann.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann { int value(); }

// FILE: p/Holder.java
package p;

import q.Lib;

public interface Holder extends Lib {
    int Y = LIB;
}

// FILE: p/Greeter.java
package p;

public interface Greeter extends Holder {
    @Ann(Y) String simpleName();
    @Ann(Holder.Y) String qualifiedName();
}

// FILE: main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate

private fun annotationValue(methodName: String): Int =
    Delegator::class.java.getDeclaredMethod(methodName).getAnnotation(Ann::class.java).value

fun box(): String {
    if (annotationValue("simpleName") != 42) return "FAIL: simple name"
    if (annotationValue("qualifiedName") != 42) return "FAIL: qualified name"
    return "OK"
}