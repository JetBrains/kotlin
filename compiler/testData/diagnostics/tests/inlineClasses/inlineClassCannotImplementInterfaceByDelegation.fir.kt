// !LANGUAGE: +InlineClasses

interface IFoo

object FooImpl : IFoo

inline class Test1(val x: Any) : IFoo by FooImpl

inline class Test2(val x: IFoo) : IFoo by x