// MEMBER_NAME_FILTER: copy
// QUERY: annotations
// value_parameter: p1: callable: pack/MyData.copy
package pack

annotation class Anno(val value: Int)

data cl<caret>ass MyData(
    @property:Anno(42) val p1: String,
    val p2: Int,
)
