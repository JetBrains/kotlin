// !LANGUAGE: +NewInference +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

// FILE: base/BaseJava.java
package base;

public abstract class BaseJava {

    public BaseJavaCase1()
    {}

    public BaseJavaCase1(Boolean b)
    { foo(b); }

    public void boo(Boolean b)
    { foo(b); }

    abstract void foo(Boolean b);
}

// FILE: Impl.kt
package implBase
import base.*

// TESTCASE NUMBER: 1

class Case1 : BaseJava() {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun foo(b: Boolean?) {}
}

fun case1() {
    val v = Case1()
    v.boo(true)
}

/*
* TESTCASE NUMBER: 2
*/
abstract class AbstractClassCase2 : BaseJava() {}

class Case2: AbstractClassCase2() {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun foo(b: Boolean?) {}
}

fun case2() {
    val v = Case2()
    v.boo(true)
}

// TESTCASE NUMBER: 3

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER!>class Case3<!> : BaseJava() {}

fun case3() {
    val v = Case3()
    v.boo(true)
}

/*
* TESTCASE NUMBER: 4
*/
abstract class AbstractClassCase4 : BaseJava() {}

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER!>class Case4<!> : AbstractClassCase4() {}

fun case4() {
    val v = Case4()
    v.boo(true)
}
