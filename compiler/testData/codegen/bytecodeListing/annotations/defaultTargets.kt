@Target(AnnotationTarget.PROPERTY)
annotation class AnnProperty

@Target(AnnotationTarget.FIELD)
annotation class AnnField

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AnnFieldProperty

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class AnnParameterProperty

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class AnnParameterField

@Target(AnnotationTarget.FIELD, AnnotationTarget.TYPE)
annotation class AnnTypeField

public class A(
        @AnnProperty @AnnField @AnnFieldProperty @AnnParameterProperty @AnnParameterField @AnnTypeField
        public val x: Int
) {

    @AnnProperty @AnnField @AnnFieldProperty @AnnParameterProperty @AnnParameterField @AnnTypeField
    public val a: Int = 1

}

@Target(AnnotationTarget.FIELD)
annotation class Anno

@Anno
val p2: Int = 4
    get() = field