// ANDROID_ANNOTATIONS
// FILE: A.java

import kotlin.annotations.jvm.internal.*;

class A {
    public void call(@ParameterName("foo") String arg) {
    }
}

// FILE: B.java
import kotlin.annotations.jvm.internal.*;

class B extends A {
    public void call(@ParameterName("bar") String arg) {
    }
}

// FILE: C.java

class C extends A {
    public void call(String arg) {
    }
}

// FILE: D.kt
open class D {
    open fun call(foo: String) {
    }
}

// FILE: E.java
import kotlin.annotations.jvm.internal.*;

class E extends D {
    public void call(@ParameterName("baz") String bar) {
    }
}

// FILE: F.java
class F extends D {
    public void call(String baaam) {
    }
}


// FILE: G.java
import kotlin.annotations.jvm.internal.*;

class G {
    public void foo(String bar, @ParameterName("foo") String baz) {
    }
}

// FILE: H.java
class H extends G {
    public void foo(String baz, String bam) {
    }
}

// FILE: test.kt
fun main() {
    val a = A()
    val b = B()
    val c = C()

    a.<!INAPPLICABLE_CANDIDATE!>call<!>(foo = "hello")
    a.call(arg = "hello")
    a.call("hello")

    b.<!INAPPLICABLE_CANDIDATE!>call<!>(foo = "hello")
    b.call(arg = "hello")
    b.<!INAPPLICABLE_CANDIDATE!>call<!>(bar = "hello")
    b.call("hello")

    c.<!INAPPLICABLE_CANDIDATE!>call<!>(foo = "hello")
    c.call(arg = "hello")
    c.call("hello")

    val e = E()
    val f = F()

    e.<!INAPPLICABLE_CANDIDATE!>call<!>(foo = "hello")
    e.call(bar = "hello")
    e.<!INAPPLICABLE_CANDIDATE!>call<!>(baz = "hello")
    e.call("hello")

    f.<!INAPPLICABLE_CANDIDATE!>call<!>(foo = "hello")
    f.call(baaam = "hello")
    f.call("hello")

    val g = G()
    val h = H()
    g.<!INAPPLICABLE_CANDIDATE!>foo<!>("ok", foo = "hohoho")
    g.foo("ok", "hohoho")
    h.<!INAPPLICABLE_CANDIDATE!>foo<!>("ok", foo = "hohoho")
    h.foo("ok", "hohoho")
}
