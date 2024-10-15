// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

interface IFoo

object FooImpl : IFoo

inline class Test1(val x: Any) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>IFoo<!> by FooImpl

inline class Test2(val x: IFoo) : IFoo by x
