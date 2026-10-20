// TARGET_BACKEND: JVM
// ISSUE: KT-90787

// FILE: p/Ann.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann { int value(); }

// FILE: p/Base.java
package p;

public interface Base { int INHERITED = 10; }

// FILE: q/Consts.java
package q;

public class Consts { public static final int ON_DEMAND = 20; }

// FILE: p/Sub.java
package p;

public class Sub implements Base { }

// FILE: p/Greeter.java
package p;

import static q.Consts.*;

public interface Greeter extends Base {
    int ENCLOSING = 30;

    @Ann(INHERITED) String inherited();
    @Ann(ON_DEMAND) String staticImportOnDemand();
    @Ann(Sub.INHERITED) String qualifiedInherited();

    interface Nested {
        @Ann(ENCLOSING) String enclosingClassField();
    }
}

// FILE: main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate
class NestedDelegator(delegate: Greeter.Nested) : Greeter.Nested by delegate

private fun value(type: Class<*>, methodName: String): Int =
    type.getDeclaredMethod(methodName).getAnnotation(Ann::class.java).value

fun box(): String {
    if (value(Delegator::class.java, "inherited") != 10) return "FAIL: inherited"
    if (value(Delegator::class.java, "staticImportOnDemand") != 20) return "FAIL: static import on demand"
    if (value(Delegator::class.java, "qualifiedInherited") != 10) return "FAIL: qualified inherited"
    if (value(NestedDelegator::class.java, "enclosingClassField") != 30) return "FAIL: enclosing class"
    return "OK"
}
