// QUERY: get: Field
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: BACKING_FIELD

class My<caret>Class(
    @all:Field
    var prop: Int,
)

@Target(AnnotationTarget.FIELD)
annotation class Field
