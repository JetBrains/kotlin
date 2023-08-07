// FILE: A.java

import kotlin.jvm.functions.FunctionN;

public class A {
    public void foo(FunctionN<?> w) { }
}

public class A2 {
    public FunctionN<?> foo() { }
}

// FILE: main.kt

class <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>B<!> : A()
class <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>B2<!> : A2()

fun foo() {
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A() {}
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A2() {}
}
