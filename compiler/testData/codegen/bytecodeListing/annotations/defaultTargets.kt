target(AnnotationTarget.PROPERTY)
annotation class AnnProperty

target(AnnotationTarget.FIELD)
annotation class AnnField

target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AnnFieldProperty

target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class AnnParameterProperty

target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class AnnParameterField

target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class AnnGetterSetter

target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY)
annotation class AnnPropertySetter

target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class AnnTypeGetter

target(AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
annotation class AnnTypeField

public class A(
        @AnnProperty @AnnField @AnnFieldProperty @AnnParameterProperty @AnnParameterField
        @AnnGetterSetter @AnnPropertySetter @AnnTypeGetter @AnnTypeField
        public val x: Int
) {

    @AnnProperty @AnnField @AnnFieldProperty @AnnParameterProperty @AnnParameterField
    @AnnGetterSetter @AnnPropertySetter @AnnTypeGetter @AnnTypeField
    public val a: Int = 1

}