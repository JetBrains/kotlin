// QUERY: contains: Property
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: BACKING_FIELD

class My<caret>Class(
    @all:Property
    var prop: Int,
)

@Target(AnnotationTarget.Property)
annotation class Property
