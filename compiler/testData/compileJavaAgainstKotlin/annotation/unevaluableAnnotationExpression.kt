// ISSUE: KT-90789

// FILE: p/Ann.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann { String value(); }

// FILE: p/Greeter.java
package p;

public interface Greeter {
    @Ann("prefix" + value()) String greet();

    static String value() { return "value"; }
}

// FILE: p/main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate
