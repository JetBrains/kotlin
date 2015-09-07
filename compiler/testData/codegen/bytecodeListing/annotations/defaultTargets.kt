@Target(AnnotationTarget.PROPERTY)
annotation class AnnProperty

@Target(AnnotationTarget.FIELD)
annotation class AnnField

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AnnFieldProperty

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class AnnParameterProperty

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class AnnParameterField

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class AnnGetterSetter

@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY)
annotation class AnnPropertySetter

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class AnnTypeGetter

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
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