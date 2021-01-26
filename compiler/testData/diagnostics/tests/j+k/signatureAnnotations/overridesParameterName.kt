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

    a.call(foo = "hello")
    a.call(<!NAMED_PARAMETER_NOT_FOUND!>arg<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.call("hello")

    b.call(<!NAMED_PARAMETER_NOT_FOUND!>foo<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    b.call(<!NAMED_PARAMETER_NOT_FOUND!>arg<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    b.call(bar = "hello")
    b.call("hello")

    c.call(foo = "hello")
    c.call(<!NAMED_PARAMETER_NOT_FOUND!>arg<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    c.call("hello")

    val e = E()
    val f = F()

    e.call(<!NAMED_PARAMETER_NOT_FOUND!>foo<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    e.call(<!NAMED_PARAMETER_NOT_FOUND!>bar<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    e.call(baz = "hello")
    e.call("hello")

    f.call(foo = "hello")
    f.call(<!NAMED_PARAMETER_NOT_FOUND!>baaam<!> = "hello"<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.call("hello")

    val g = G()
    val h = H()
    g.foo("ok", <!NAMED_ARGUMENTS_NOT_ALLOWED!>foo<!> = "hohoho")
    g.foo("ok", "hohoho")
    h.foo("ok", <!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>foo<!> = "hohoho"<!NO_VALUE_FOR_PARAMETER!>)<!>
    h.foo("ok", "hohoho")
}
