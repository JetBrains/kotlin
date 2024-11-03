// FIR_IDENTICAL

@RequiresOptIn
annotation class ApiMarkerA

@Target(AnnotationTarget.ANNOTATION_CLASS)
@RequiresOptIn
annotation class ApiMarkerB

@Target(AnnotationTarget.CLASS)
@RequiresOptIn
annotation class ApiMarkerC

@SubclassOptInRequired(ApiMarkerA::class)
open class A

@SubclassOptInRequired(<!SUBCLASS_OPT_IN_MARKER_ON_WRONG_TARGET("ApiMarkerB")!>ApiMarkerB::class<!>)
open class B

@SubclassOptInRequired(ApiMarkerC::class)
open class C
