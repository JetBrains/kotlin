interface Base

@CompileTimeCalculation
open class A : Base

@CompileTimeCalculation
class B : A()

const val a1 = 1 is Int
const val a2 = 2 !is Int

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
