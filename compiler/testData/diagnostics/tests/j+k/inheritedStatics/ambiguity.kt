// FILE: base/Base.java
package base;

public class Base {
    public static final int HIDDEN = 1;
}

// FILE: derived/Interface.java
package derived;

public interface Interface {
    long HIDDEN = 1L;
}

// FILE: derived/Derived.java

package derived;

public class Derived extends base.Base implements Interface {
}

// FILE: test.kt

import base.*
import derived.*

fun test() {
    Base.HIDDEN: Int
    Interface.HIDDEN: Long

    Derived.<!OVERLOAD_RESOLUTION_AMBIGUITY!>HIDDEN<!>
}