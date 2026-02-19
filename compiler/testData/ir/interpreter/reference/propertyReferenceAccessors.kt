import kotlin.*

@CompileTimeCalculation
class A(val a: Int)

const val propertyGetterInvoke = <!EVALUATED: `1`!>A::a.getter(A(1))<!>

const val propertyWithReceiverGetterInvoke = <!EVALUATED: `11`!>A(11)::a.getter()<!>

@CompileTimeCalculation
class B(var b: Int)

const val mutablePropertyGetterInvoke = <!EVALUATED: `1`!>B::b.getter(B(1))<!>
const val mutablePropertySetterInvoke = <!EVALUATED: `3`!>B(2).apply { B::b.setter(this, 3) }.b<!>

const val mutablePropertyWithReceiverGetterInvoke = <!EVALUATED: `11`!>B(11)::b.getter()<!>
const val mutablePropertyWithReceiverSetterInvoke = <!EVALUATED: `13`!>B(12).apply { this::b.setter(13) }.b<!>
