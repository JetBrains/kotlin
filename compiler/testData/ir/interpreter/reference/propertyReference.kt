import kotlin.*

@CompileTimeCalculation
class A(val a: Int)

const val propertyName = A::a.<!EVALUATED: `a`!>name<!>
const val propertyGet = A::a.<!EVALUATED: `1`!>get(A(1))<!>
const val propertyInvoke = A::a.<!EVALUATED: `2`!>invoke(A(2))<!>

const val propertyWithReceiverName = A(10)::a.<!EVALUATED: `a`!>name<!>
const val propertyWithReceiverGet = A(11)::a.<!EVALUATED: `11`!>get()<!>
const val propertyWithReceiverInvoke = A(12)::a.<!EVALUATED: `12`!>invoke()<!>

@CompileTimeCalculation
class B(var b: Int)

const val mutablePropertyName = B::b.<!EVALUATED: `b`!>name<!>
const val mutablePropertyGet = B::b.<!EVALUATED: `1`!>get(B(1))<!>
const val mutablePropertySet = B(2).apply { B::b.set(this, 3) }.<!EVALUATED: `3`!>b<!>
const val mutablePropertyInvoke = B::b.<!EVALUATED: `4`!>invoke(B(4))<!>

const val mutablePropertyWithReceiverName = B(10)::b.<!EVALUATED: `b`!>name<!>
const val mutablePropertyWithReceiverGet = B(11)::b.<!EVALUATED: `11`!>get()<!>
const val mutablePropertyWithReceiverSet = B(12).apply { this::b.set(13) }.<!EVALUATED: `13`!>b<!>
const val mutablePropertyWithReceiverInvoke = B(14)::b.<!EVALUATED: `14`!>invoke()<!>

@CompileTimeCalculation
var <T> T.bar : T
    get() = this
    set(value) { }

const val barToString = String::bar.<!EVALUATED: `var T.bar: T`!>toString()<!>
