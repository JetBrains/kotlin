// !LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
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

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case1<!> : BaseJava() {}

fun case1() {
    val v = Case1()
    v.boo(true)
}

/*
* TESTCASE NUMBER: 2
*/
abstract class AbstractClassCase2 : BaseJava() {}

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case2<!> : AbstractClassCase2() {}

fun case2() {
    val v = Case2()
    v.boo(true)
}
