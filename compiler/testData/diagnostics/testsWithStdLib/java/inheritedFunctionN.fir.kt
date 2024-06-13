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

<!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>class B1<!> : A1()
<!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>class B2<!> : A2()

fun foo() {
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A1() {}
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A2() {}
}
