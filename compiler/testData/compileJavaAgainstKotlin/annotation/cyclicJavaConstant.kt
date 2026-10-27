// ISSUE: KT-90790

// FILE: p/Ann.java
package p;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Ann { int value(); }

// FILE: p/Greeter.java
package p;

public interface Greeter {
    int A = Greeter.B + 1;
    int B = Greeter.A + 1;

    @Ann(A + 0) String greet();
}

// FILE: p/main.kt
package p

class Delegator(delegate: Greeter) : Greeter by delegate
