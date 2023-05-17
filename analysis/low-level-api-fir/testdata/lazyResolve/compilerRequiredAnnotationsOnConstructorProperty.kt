// MEMBER_NAME_FILTER: i
annotation class Anno(val s: String)

class <caret>A @Deprecated("constructor") @Anno("constructor") constructor(
    @param:[Deprecated("param") Anno("param")]
    @field:[Deprecated("field") Anno("field")]
    @property:Deprecated("property") @property:Anno("property")
    val i: Int,
    @Deprecated("parameter") @Anno("parameter")
    b: String
)