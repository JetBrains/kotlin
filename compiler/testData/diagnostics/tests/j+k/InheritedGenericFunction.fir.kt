// FILE: p/A.java

package p;

public interface A {
    <TA> void foo(TA p);
}

// FILE: p/B.java

package p;

public class B implements A {
    <TB> void foo(TB p) {}
}

// FILE: p/C.java

package p;

public class C extends B implements A {
}

// FILE: k.kt

import p.*

abstract class K: C() {

}
