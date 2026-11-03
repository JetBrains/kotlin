// QUERY: get: Get
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: GETTER

class My<caret>Class(
    @all:Get
    var prop: Int,
)

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Get
