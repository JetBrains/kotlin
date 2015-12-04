// FILE: A.java
import java.util.*;
public interface A<T> {
    <E extends CharSequence> E foo(T x, List<? extends T> y);
}

// FILE: B.java
import java.util.*;
public interface B extends A {
    @Override
    public String foo(Object x, List y);
}

// FILE: C.java
import java.util.*;
public abstract class C {
    <E extends CharSequence, F extends E> E bar(F x, List<Map<E, F>> y);
}

// FILE: D.java
import java.util.*;
public class D extends C {
    @Override
    public String bar(CharSequence x, List y) {
        return null;
    }
}

// FILE: main.kt

class E : D(), B {
    override fun foo(x: Any, y: List<Any?>): String = ""
    override fun bar(x: CharSequence?, y: List<*>?): String = ""
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class E2<!> : B {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(x: Any, y: List<String?>): String = ""
}


class F : D()
