// FIR_IDENTICAL
// FILE: B.java
import java.util.List;

public class B implements X {
    @Override
    List foo(List l) {
        return super.foo(l);
    }
}

// FILE: 1.kt

interface X {
    fun foo(l: MutableList<Int>): List<String>?
}

internal <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>C<!><!> : B()
