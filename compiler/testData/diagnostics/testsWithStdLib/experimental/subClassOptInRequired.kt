// FIR_IDENTICAL
@RequiresOptIn
annotation class Marker

@SubclassOptInRequired(Marker::class)
open class Base

@Marker
class DerivedFirst : Base()

@OptIn(Marker::class)
class DerivedSecond : Base()

@SubclassOptInRequired(Marker::class)
open class DerivedThird : Base()

open class DerivedFourth : <!OPT_IN_USAGE_ERROR!>Base<!>()

class GrandDerivedThird : <!OPT_IN_USAGE_ERROR!>DerivedThird<!>()

// Question: should we have an error also here?
class GrandDerivedFourth : DerivedFourth()

@Marker
open class Marked

@SubclassOptInRequired(Marker::class)
open class DerivedMarked : <!OPT_IN_USAGE_ERROR!>Marked<!>()

fun test() {
    val b = Base()
    val d1 = <!OPT_IN_USAGE_ERROR!>DerivedFirst<!>()
    val d2 = DerivedSecond()
    val d3 = DerivedThird()
    val d4 = DerivedFourth()
}

fun test2(b: Base, g: Generic<Base>) {
    object : <!OPT_IN_USAGE_ERROR!>Base<!>() {}
}

open class Generic<T>

class DerivedGeneric : Generic<Base>()

@SubclassOptInRequired(Marker::class)
interface BaseInterface

interface DerivedInterface : <!OPT_IN_USAGE_ERROR!>BaseInterface<!>

class Delegated(val bi: BaseInterface) : <!OPT_IN_USAGE_ERROR!>BaseInterface<!> by bi
