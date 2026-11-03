// QUERY: annotations
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: BACKING_FIELD

class My<caret>Class {
    @property:Property
    @Field
    @all:Everything
    var prop: Int = 0
}

@Target(AnnotationTarget.PROPERTY)
annotation class Property

@Target(AnnotationTarget.FIELD)
annotation class Field

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class Everything
