// FIR_IDENTICAL

import java.io.Serializable

interface Test1 {
    <!CONFLICTING_OVERLOADS!>fun <T> foo(t: T)<!> where T : Cloneable, T : Serializable
    <!CONFLICTING_OVERLOADS!>@Deprecated("foo", level = DeprecationLevel.HIDDEN)
    fun <T> foo(t: T)<!> where T : Serializable, T : Cloneable
}


interface I1
interface I2 : I1

interface Test2 {
    <!CONFLICTING_OVERLOADS!>fun <T> foo(t: T)<!> where T : I1, T : I2
    <!CONFLICTING_OVERLOADS!>@Deprecated("foo", level = DeprecationLevel.HIDDEN)
    fun <T> foo(t: T)<!> where T : I2, T : I1
}
