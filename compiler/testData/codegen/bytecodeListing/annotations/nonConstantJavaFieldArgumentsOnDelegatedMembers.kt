// TARGET_BACKEND: JVM
// ISSUE: KT-90363
// DISABLE_JAVA_FACADE
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
    public static int FIELD = 1;
    public static final int NON_CONSTANT = Integer.parseInt("2");
}

// FILE: p/Greeter.java
package p;

public interface Greeter {
    @Ann(Other.FIELD) String nonFinalField();
    @Ann(Other.NON_CONSTANT) String finalNonConstantField();
}

// FILE: main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate