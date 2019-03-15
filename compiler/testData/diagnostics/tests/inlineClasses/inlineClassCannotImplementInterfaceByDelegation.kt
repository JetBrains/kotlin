// !LANGUAGE: +InlineClasses

interface IFoo

object FooImpl : IFoo

inline class Test1(val x: Any) : <!INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by FooImpl<!>

inline class Test2(val x: IFoo) : <!INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by x<!>