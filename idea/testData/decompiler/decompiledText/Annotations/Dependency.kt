package dependency

@Target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A(val s: String)

@Target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class B(val i: Int)

@Target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER)
annotation class C
