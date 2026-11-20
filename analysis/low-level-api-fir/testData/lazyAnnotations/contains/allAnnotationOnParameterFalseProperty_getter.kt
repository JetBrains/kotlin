// QUERY: contains: Property
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: GETTER

class My<caret>Class(
    @all:Property
    var prop: Int,
)

@Target(AnnotationTarget.PROPERTY)
annotation class Property
