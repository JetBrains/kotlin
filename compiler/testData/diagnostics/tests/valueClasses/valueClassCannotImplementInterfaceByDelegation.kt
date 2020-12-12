// !LANGUAGE: +InlineClasses

package kotlin.jvm

annotation class JvmInline

interface IFoo

object FooImpl : IFoo

@JvmInline
value class Test1(val x: Any) : <!INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by FooImpl<!>

@JvmInline
value class Test2(val x: IFoo) : <!INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by x<!>