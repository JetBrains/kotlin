package override.generics

trait MyTrait<T> {
    fun foo(t: T) : T
}

abstract class MyAbstractClass<T> {
    abstract fun bar(t: T) : T
    abstract val pr : T
}

trait MyProps<T> {
    val p : T
}

open class MyGenericClass<T>(t : T) : MyTrait<T>, MyAbstractClass<T>(), MyProps<T> {
    override fun foo(t: T) = t
    override fun bar(t: T) = t
    override val p : T = t
    override val pr : T = t
}

class MyChildClass() : MyGenericClass<Int>(1) {}
class MyChildClass1<T>(t : T) : MyGenericClass<T>(t) {}
class MyChildClass2<T>(t : T) : MyGenericClass<T>(t) {
    <!VIRTUAL_MEMBER_HIDDEN!>fun foo(t: T)<!> = t
    <!VIRTUAL_MEMBER_HIDDEN!>val pr : T<!> = t
    override fun bar(t: T) = t
    override val p : T = t
}

open class MyClass() : MyTrait<Int>, MyAbstractClass<String>() {
    override fun foo(t: Int) = t
    override fun bar(t: String) = t
    override val pr : String = "1"
}

abstract class MyAbstractClass1 : MyTrait<Int>, MyAbstractClass<String>() {
    override fun foo(t: Int) = t
    override fun bar(t: String) = t
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalGenericClass1<!><T> : MyTrait<T>, MyAbstractClass<T>() {}
class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalGenericClass2<!><T, R>(r : R) : MyTrait<T>, MyAbstractClass<R>() {
    <!ACCIDENTAL_OVERRIDE!><!NOTHING_TO_OVERRIDE!>override<!> fun foo(r: R)<!> = r
    <!CONFLICTING_OVERLOADS!><!NOTHING_TO_OVERRIDE!>override<!> val <T> pr : R<!> = r
}
class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalClass1<!> : MyTrait<Int>, MyAbstractClass<String>() {}
abstract class MyLegalAbstractClass1 : MyTrait<Int>, MyAbstractClass<String>() {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>MyIllegalClass2<!><T>(t : T) : MyTrait<Int>, MyAbstractClass<Int>() {
    fun foo(t: T) = t
    fun bar(t: T) = t
    <!CONFLICTING_OVERLOADS!>val <R> pr : T<!> = t
}
abstract class MyLegalAbstractClass2<T>(t : T) : MyTrait<Int>, MyAbstractClass<Int>() {
    fun foo(t: T) = t
    fun bar(t: T) = t
    <!CONFLICTING_OVERLOADS!>val <R> pr : T<!> = t
}