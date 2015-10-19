// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -PARAMETER_NAME_CHANGED_ON_OVERRIDE
// FILE: A.java
import java.util.*;

abstract public class A<T> implements java.util.Collection<T> {
    public boolean containsAll(Collection<?> x) {return false;}
}

// FILE: B.java
import java.util.*;

abstract public class B implements java.util.Collection<String> {
    public boolean containsAll(Collection<?> x) {return false;}
}

// FILE: IC.java
import java.util.*;

public interface IC implements java.util.Collection<String> {
    public boolean containsAll(Collection<?> x);
}

// FILE: main.kt
abstract class KA<T> : java.util.AbstractList<T>() {
    override fun containsAll(x: Collection<T>) = false
}

abstract class KB : java.util.AbstractList<String>(), IC {
    override fun containsAll(elements: Collection<String>) = false
}

fun foo(
        a: A<String>, b: B, ic: IC,
        ka: KA<String>, kb: KB,
        al: java.util.ArrayList<String>,
        cs: Collection<String>, ca: Collection<Any?>
) {
    a.containsAll(cs)
    a.containsAll(<!TYPE_MISMATCH!>ca<!>)

    b.containsAll(cs)
    b.containsAll(<!TYPE_MISMATCH!>ca<!>)

    ic.containsAll(cs)
    ic.containsAll(<!TYPE_MISMATCH!>ca<!>)

    ka.containsAll(cs)
    ka.containsAll(<!TYPE_MISMATCH!>ca<!>)

    kb.containsAll(cs)
    kb.containsAll(<!TYPE_MISMATCH!>ca<!>)

    al.containsAll(cs)
    al.containsAll(<!TYPE_MISMATCH!>ca<!>)
}
