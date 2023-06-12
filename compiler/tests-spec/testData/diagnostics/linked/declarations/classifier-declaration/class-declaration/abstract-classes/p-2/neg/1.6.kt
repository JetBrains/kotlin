// FIR_IDENTICAL
// !LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * NUMBER: 6
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * ISSUES: KT-27825
 */


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

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case3<!> : BaseJava() {}

fun case3() {
    val v = Case3()
    v.boo(true)
}

/*
* TESTCASE NUMBER: 4
*/
abstract class AbstractClassCase4 : BaseJava() {}

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case4<!> : AbstractClassCase4() {}

fun case4() {
    val v = Case4()
    v.boo(true)
}

