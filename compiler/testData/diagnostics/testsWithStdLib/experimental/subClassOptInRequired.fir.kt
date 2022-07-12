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

open class DerivedFourth : Base()

class GrandDerivedThird : DerivedThird()

// Question: should we have an error also here?
class GrandDerivedFourth : DerivedFourth()

@Marker
open class Marked

@SubclassOptInRequired(Marker::class)
class DerivedMarked : <!OPT_IN_USAGE_ERROR!>Marked<!>()

fun test() {
    val b = Base()
    val d1 = <!OPT_IN_USAGE_ERROR!>DerivedFirst<!>()
    val d2 = DerivedSecond()
    val d3 = DerivedThird()
    val d4 = DerivedFourth()
}
