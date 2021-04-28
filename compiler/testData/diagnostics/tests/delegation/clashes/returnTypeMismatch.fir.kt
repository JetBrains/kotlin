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

interface IAny {
    fun foo(): Any
}

class CAny : IAny {
    override fun foo(): Any = null!!
}

interface IGeneric<T> {
    fun foo(): T
}

class CGeneric<T> : IGeneric<T> {
    override fun foo(): T {
        throw UnsupportedOperationException()
    }
}

abstract class Test1 : IStr by CStr(), IInt

abstract class Test2 : IStr, IInt by CInt()

abstract <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Test3<!> : IStr by CStr(), IInt by CInt()

abstract class Test4 : IStr by CStr(), IGeneric<String>

abstract class Test5 : IStr by CStr(), IGeneric<Any>

abstract class Test6 : IStr by CStr(), IGeneric<Int>

abstract class Test7 : IGeneric<String> by CGeneric<String>(), IStr

abstract class Test8 : IGeneric<String> by CGeneric<String>(), IInt

// Can't test due to https://youtrack.jetbrains.com/issue/KT-10258
// abstract class Test9 : IGeneric<String> by CGeneric<String>(), IGeneric<Int>

abstract <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Test10<!> : IInt by CInt(), IStr by CStr(), IAny by CAny()

abstract <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class Test11<!> : IInt, IStr by CStr(), IAny by CAny()

abstract class Test12 : IInt, IStr, IAny by CAny()

