annotation class DummyAnnotation

@RequiresOptIn
annotation class NotDummyAnnotation

<!SUBCLASS_OPT_ARGUMENT_IS_NOT_MARKER!>@SubclassOptInRequired(DummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarkerA

<!SUBCLASS_OPT_ARGUMENT_IS_NOT_MARKER("DummyAnnotation")!>@SubclassOptInRequired(DummyAnnotation::class, NotDummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarkerB
