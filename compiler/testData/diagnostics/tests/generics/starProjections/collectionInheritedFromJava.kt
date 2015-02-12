// FILE: p/Base.java

package p;

import java.util.*;

public class Base<T> {
    void coll(Collection<?> r) {}
}

// FILE: k.kt
package p

class Derived: p.Base<String>()