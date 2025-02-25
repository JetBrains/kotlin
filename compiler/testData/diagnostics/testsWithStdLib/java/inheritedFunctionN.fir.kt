// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
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

// FILE: A3.java

import kotlin.jvm.functions.FunctionN;

public class A3 implements I {
    @Override
    public void foo(FunctionN<?> x) { }
}

// FILE: main.kt
import kotlin.jvm.functions.FunctionN

interface I {
    context(x: FunctionN<*>)
    fun foo()
}

<!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>class B1<!> : A1()
<!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>class B2<!> : A2()
<!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>class B3<!> : A3()

fun foo() {
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A1() {}
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A2() {}
    <!UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION!>object<!> : A3() {}
}
