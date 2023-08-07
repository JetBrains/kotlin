
// FILE: A.java

import kotlin.jvm.functions.FunctionN;

public class A {
    public void foo(FunctionN<?> w) { }
}

// FILE: main.kt

<!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>class B<!> : A()

fun foo() {
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A() {}
}
