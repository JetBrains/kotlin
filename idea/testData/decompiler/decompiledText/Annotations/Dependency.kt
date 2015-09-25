package dependency

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A(val s: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class B(val i: Int)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER)
annotation class C
