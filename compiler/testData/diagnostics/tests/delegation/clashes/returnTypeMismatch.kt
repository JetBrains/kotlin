interface IStr {
    fun foo(): String
}

class CStr : IStr {
    override fun foo(): String = ""
}

interface IInt {
    fun foo(): Int
}

class CInt : IInt {
    override fun foo(): Int = 42
}

interface IGeneric<T> {
    fun foo(): T
}

class CGeneric<T> : IGeneric<T> {
    override fun foo(): T {
        throw UnsupportedOperationException()
    }
}

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE, RETURN_TYPE_MISMATCH_ON_OVERRIDE_BY_DELEGATION!>class Test1<!> : IStr by CStr(), IInt

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE, RETURN_TYPE_MISMATCH_ON_OVERRIDE_BY_DELEGATION!>class Test2<!> : IStr, IInt by CInt()

abstract <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED, RETURN_TYPE_MISMATCH_ON_INHERITANCE, RETURN_TYPE_MISMATCH_ON_OVERRIDE_BY_DELEGATION!>class Test3<!> : IStr by CStr(), IInt by CInt()

abstract class Test4 : IStr by CStr(), IGeneric<String>

abstract class Test5 : IStr by CStr(), IGeneric<Any>

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE, RETURN_TYPE_MISMATCH_ON_OVERRIDE_BY_DELEGATION!>class Test6<!> : IStr by CStr(), IGeneric<Int>

abstract class Test7 : IGeneric<String> by CGeneric<String>(), IStr

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE, RETURN_TYPE_MISMATCH_ON_OVERRIDE_BY_DELEGATION!>class Test8<!> : IGeneric<String> by CGeneric<String>(), IInt

// Can't test right now due to https://youtrack.jetbrains.com/issue/KT-10258
// abstract class Test9 : IGeneric<String> by CGeneric<String>(), IGeneric<Int>
