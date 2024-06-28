package two

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyExplicitly

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyImplicitly

@Target(AnnotationTarget.FIELD)
annotation class FieldExplicitly

@Target(AnnotationTarget.FIELD)
annotation class FieldImplicitly

enum class AnnotationsOnEnumEntry(i: Int = 1) {
    @PropertyImplicitly
    @FieldImplicitly
    @field:FieldExplicitly
    EntryWithoutConstructor,

    @PropertyImplicitly
    @FieldImplicitly
    EntryWithConstructor(5),

    EntryWithConstructor2(6);

    fun foo() = Unit
}
