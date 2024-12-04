package test

import kotlin.collections.*

@CompileTimeCalculation
class A(val a: Int) {
    val String.size: Int
        get() = this.length * a
}

const val kproperty2Get = <!EVALUATED: `6`!>A::class.members.toList()[1].call(A(2), "123").toString()<!>
