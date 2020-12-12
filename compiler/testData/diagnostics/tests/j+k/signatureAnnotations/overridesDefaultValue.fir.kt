// FILE: A.java
// ANDROID_ANNOTATIONS

import kotlin.annotations.jvm.internal.*;

class A {
    public void first(@DefaultValue("42") int arg) {
    }
}

// FILE: B.java
class B {
    public void first(int arg) {
    }
}

// FILE: C.java
import kotlin.annotations.jvm.internal.*;

class C extends A {
    public void first(@DefaultValue("73") int arg) {
    }
}

// FILE: D.java
import kotlin.internal.*;

class D extends B {
    public void first(@DefaultValue("37") int arg) {
    }
}

// FILE: E.java
import kotlin.annotations.jvm.internal.*;

class E extends A {
    public void first(int arg) {
    }
}

// FILE: F.kt
open class F {
    open fun foo(x: String = "0") {
    }
}

// FILE: G.java
class G extends F {
    public void foo(String y) {
    }
}

// FILE: K.java
import kotlin.annotations.jvm.internal.*;

public interface K {
    public void foo(@DefaultValue("1") String x) { }
}

// FILE: L.java
import kotlin.annotations.jvm.internal.*;

public interface L {
    public void foo(@DefaultValue("1") String x) { }
}

// FILE: M.java
public class M implements K, L {
    public void foo(String x) {
    }
}

// FILE: main.kt
fun main() {
    val a = A()
    val c = C()
    val d = D()
    val e = E()

    val ac: A = C()
    val bd: B = D()

    a.first()
    c.first()
    ac.first()

    d.<!INAPPLICABLE_CANDIDATE!>first<!>()
    bd.<!INAPPLICABLE_CANDIDATE!>first<!>()

    e.first()

    val g = G()
    g.foo()
    g.foo("ok")

    val m = M()
    m.foo()

}

