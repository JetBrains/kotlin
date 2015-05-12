open class Required(val value: String)

interface First : <!TRAIT_WITH_SUPERCLASS!>Required<!>

interface Second : First

<!UNMET_TRAIT_REQUIREMENT!>class Y<!> : Second
class Z : Second, Required(":o)")
