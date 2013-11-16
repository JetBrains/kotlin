// FILE: base/Base.java
package base;

public class Base {
    public static final int HIDDEN = 1;
}

// FILE: derived/Derived.java

package derived;

public class Derived extends base.Base {
    private static final String HIDDEN = "";
}

// FILE: derived/Interface.java
package derived;

public interface Interface {
    long HIDDEN = 1L;
}

// FILE: derived/Derived1.java

package derived;

public class Derived1 extends Derived implements Interface {
}

// FILE: test.kt

import base.*
import derived.*

fun test() {
    Base.HIDDEN: Int
    Interface.HIDDEN: Long

    Derived.<!UNRESOLVED_REFERENCE!>HIDDEN<!>

    Derived1.HIDDEN: Long
}