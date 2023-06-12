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
 * NUMBER: 7
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * ISSUES: KT-27825
 */

// MODULE: base
// FILE: BaseKotlin.kt
package base
abstract class BaseKotlin() {

    fun boo(b: Boolean?) {
        foo(b)
    }

    internal abstract fun foo(b: Boolean?)
}


//MODULE: implBase(base)
//FILE: Impl.kt


package implBase
import base.*


// TESTCASE NUMBER: 1

class Case1 : BaseKotlin() {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun foo(b: Boolean?) {}
}

fun case1() {
    val v = Case1()
    v.boo(true)

    val o = object :  BaseKotlin() {
        <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun foo(b: Boolean?) {}
    }
}

/*
* TESTCASE NUMBER: 2
*/
abstract class AbstractClassCase2 : BaseKotlin() {}

class Case2: AbstractClassCase2() {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun foo(b: Boolean?) {}
}

fun case2() {
    val v = Case2()
    v.boo(true)

    val o = object : AbstractClassCase2() {
        <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun foo(b: Boolean?) {}
    }
}

// TESTCASE NUMBER: 3

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case3<!> : BaseKotlin() {}

fun case3() {
    val v = Case3()
    v.boo(true)

    val o = <!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>object<!> : BaseKotlin() {}
}

/*
* TESTCASE NUMBER: 4
*/
abstract class AbstractClassCase4 : BaseKotlin() {}

<!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class Case4<!> : AbstractClassCase4() {}

fun case4() {
    val v = Case4()
    v.boo(true)
    val o = <!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>object<!> : AbstractClassCase4() {}

}




