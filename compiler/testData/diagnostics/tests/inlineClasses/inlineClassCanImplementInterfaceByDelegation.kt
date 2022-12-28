// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses, +InlineClassImplementationByDelegation
// SKIP_TXT

interface IFoo

object FooImpl : IFoo

class CFoo : IFoo

val c = CFoo()

inline class Test1(val x: Any) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by FooImpl<!>

inline class Test2(val x: IFoo) : IFoo by x

inline class Test3(val x: IFoo) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by CFoo()<!>

inline class Test4(val x: IFoo) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo by c<!>
