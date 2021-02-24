// !LANGUAGE: +NewInference +ProhibitInvisibleAbstractMethodsInSuperclasses
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

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
    override fun foo(b: Boolean?) {}
}

fun case1() {
    val v = Case1()
    v.boo(true)

    val o = object :  BaseKotlin() {
        override fun foo(b: Boolean?) {}
    }
}

/*
* TESTCASE NUMBER: 2
*/
abstract class AbstractClassCase2 : BaseKotlin() {}

class Case2: AbstractClassCase2() {
    override fun foo(b: Boolean?) {}
}

fun case2() {
    val v = Case2()
    v.boo(true)

    val o = object : AbstractClassCase2() {
        override fun foo(b: Boolean?) {}
    }
}

// TESTCASE NUMBER: 3

class Case3 : BaseKotlin() {}

fun case3() {
    val v = Case3()
    v.boo(true)

    val o = object : BaseKotlin() {}
}

/*
* TESTCASE NUMBER: 4
*/
abstract class AbstractClassCase4 : BaseKotlin() {}

class Case4 : AbstractClassCase4() {}

fun case4() {
    val v = Case4()
    v.boo(true)
    val o = object : AbstractClassCase4() {}

}
