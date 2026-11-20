// QUERY: get: Everything
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: SETTER

class My<caret>Class(
    @all:Everything
    var prop: Int,
)

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class Everything
