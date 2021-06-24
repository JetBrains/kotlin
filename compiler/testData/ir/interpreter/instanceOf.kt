// IGNORE_BACKEND: JVM_IR
interface Base

@CompileTimeCalculation
open class A : Base

@CompileTimeCalculation
class B : A()

const val a1 = <!EVALUATED: `true`!>{ 1 is Int }()<!> // avoid evaluation by native interpreter
const val a2 = <!EVALUATED: `false`!>{ 2 !is Int }()<!>

const val b1 = <!EVALUATED: `true`!>A() is Base<!>
const val b2 = <!EVALUATED: `false`!>A() !is Base<!>
const val b3 = <!EVALUATED: `true`!>A() is A<!>
const val b4 = <!EVALUATED: `false`!>A() !is A<!>

const val c1 = <!EVALUATED: `true`!>B() is Base<!>
const val c2 = <!EVALUATED: `false`!>B() !is Base<!>
const val c3 = <!EVALUATED: `true`!>B() is A<!>
const val c4 = <!EVALUATED: `false`!>B() !is A<!>
const val c5 = <!EVALUATED: `true`!>B() is B<!>
const val c6 = <!EVALUATED: `false`!>B() !is B<!>

@CompileTimeCalculation
fun foo(): Unit {}
@CompileTimeCalculation
fun bar(p1: Int): Unit {}

const val d1 =<!EVALUATED: `true`!>::foo is kotlin.reflect.KFunction<*><!>
const val d2 =<!EVALUATED: `true`!>::foo is Function0<*><!>
const val d3 =<!EVALUATED: `false`!>::foo is Function1<*, *><!>
const val d4 =<!EVALUATED: `true`!>::bar is kotlin.reflect.KFunction<*><!>
const val d5 =<!EVALUATED: `false`!>::bar is Function0<*><!>
const val d6 =<!EVALUATED: `true`!>::bar is Function1<*, *><!>
