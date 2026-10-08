// ISSUE: KT-89993
// LANGUAGE: +FullValueClasses

abstract value class Base(b: Int)

abstract value class PairBase(a: Int, b: Int)

abstract value class GenericBase<T>(t: T)

fun traced(value: Int): Int = value

value class PlainArgument(val x: Int) : Base(x)

value class IfArgument(val x: Int) : Base(if (x > 0) 1 else 2)

value class ElvisArgument(val x: Int?) : Base(x ?: 0)

value class GenericArgument(val x: Int) : GenericBase<Int>(x)

value class ReorderedArguments(val x: Int) : PairBase(b = traced(x), a = traced(3))
