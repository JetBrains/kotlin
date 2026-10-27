// QUERY: contains: Field
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: BACKING_FIELD

class My<caret>Class {
    @property:Property
    @Field
    var prop: Int = 0
}

@Target(AnnotationTarget.PROPERTY)
annotation class Property

@Target(AnnotationTarget.FIELD)
annotation class Field
