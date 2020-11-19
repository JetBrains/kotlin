// !LANGUAGE: +InlineClasses

package kotlin

annotation class JvmInline

interface IFoo

object FooImpl : IFoo

@JvmInline
value class Test1(val x: Any) : IFoo by FooImpl

@JvmInline
value class Test2(val x: IFoo) : IFoo by x