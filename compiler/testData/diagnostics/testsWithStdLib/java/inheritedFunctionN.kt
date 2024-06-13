// FILE: A1.java

import kotlin.jvm.functions.FunctionN;

public class A1 {
    public void foo(FunctionN<?> w) { }
}

// FILE: A2.java

import kotlin.jvm.functions.FunctionN;

public class A2 {
    public FunctionN<?> foo() { }
}

// FILE: main.kt

class <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>B1<!> : A1()
class <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>B2<!> : A2()

fun foo() {
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A1() {}
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A2() {}
}
