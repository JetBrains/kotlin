open class Required(val value: String)

trait First : <!TRAIT_WITH_SUPERCLASS!>Required<!>

trait Second : First

<!UNMET_TRAIT_REQUIREMENT!>class Y<!> : Second
class Z : Second, Required(":o)")
