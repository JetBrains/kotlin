open class Required(val value: String)

trait First : Required

trait Second : First

class <!UNMET_TRAIT_REQUIREMENT!>Y<!> : Second
class Z : Second, Required(":o)")
