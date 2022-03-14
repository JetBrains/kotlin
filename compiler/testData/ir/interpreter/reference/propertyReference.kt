import kotlin.*

@CompileTimeCalculation
class A(val a: Int)

const val propertyName = <!EVALUATED: `a`!>A::a.name<!>
const val propertyGet = <!EVALUATED: `1`!>A::a.get(A(1))<!>
const val propertyInvoke = <!EVALUATED: `2`!>A::a.invoke(A(2))<!>

const val propertyWithReceiverName = <!EVALUATED: `a`!>A(10)::a.name<!>
const val propertyWithReceiverGet = <!EVALUATED: `11`!>A(11)::a.get()<!>
const val propertyWithReceiverInvoke = <!EVALUATED: `12`!>A(12)::a.invoke()<!>

@CompileTimeCalculation
class B(var b: Int)

const val mutablePropertyName = <!EVALUATED: `b`!>B::b.name<!>
const val mutablePropertyGet = <!EVALUATED: `1`!>B::b.get(B(1))<!>
const val mutablePropertySet = <!EVALUATED: `3`!>B(2).apply { B::b.set(this, 3) }.b<!>
const val mutablePropertyInvoke = <!EVALUATED: `4`!>B::b.invoke(B(4))<!>

const val mutablePropertyWithReceiverName = <!EVALUATED: `b`!>B(10)::b.name<!>
const val mutablePropertyWithReceiverGet = <!EVALUATED: `11`!>B(11)::b.get()<!>
const val mutablePropertyWithReceiverSet = <!EVALUATED: `13`!>B(12).apply { this::b.set(13) }.b<!>
const val mutablePropertyWithReceiverInvoke = <!EVALUATED: `14`!>B(14)::b.invoke()<!>

@CompileTimeCalculation
var <T> T.bar : T
    get() = this
    set(value) { }

const val barToString = <!EVALUATED: `var T.bar: T`!>String::bar.toString()<!>
