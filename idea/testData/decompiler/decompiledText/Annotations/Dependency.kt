package dependency

target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
       AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A(val s: String)

target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
       AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class B(val i: Int)

target(AnnotationTarget.CLASSIFIER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY,
       AnnotationTarget.VALUE_PARAMETER)
annotation class C
