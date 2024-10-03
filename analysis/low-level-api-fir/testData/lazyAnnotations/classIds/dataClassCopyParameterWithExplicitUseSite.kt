// MEMBER_NAME_FILTER: copy
// QUERY: classIds
// value_parameter: p1: callable: pack/MyData.copy
package pack

annotation class Anno(val value: Int)

data cl<caret>ass MyData(
    @param:Anno(42) val p1: String,
    val p2: Int,
)
