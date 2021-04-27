// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Base

class Impl1 : Base

class Impl2 : Base

operator fun Base.plus(arg: Base) = Impl1()

operator fun Impl2.plus(arg: Base) = Impl2()

operator fun Base.plus(arg: Impl2) = Impl2()

operator fun Base.unaryMinus() = Impl1()

operator fun Impl2.unaryMinus() = Impl2()

// See also KT-10384: in non-error functions, as is necessary!
fun add1(x: Impl2, y: Base): Impl1 = x as Base + y

fun error1(x: Impl2, y: Base): Impl1 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>x + y<!>

fun add2(x: Base, y: Impl2): Impl1 = x + y as Base

fun error2(x: Base, y: Impl2): Impl1 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>x + y<!>

fun minus3(x: Impl2): Impl1 = -(x as Base)

fun error3(x: Impl2): Impl1 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>-x<!>
