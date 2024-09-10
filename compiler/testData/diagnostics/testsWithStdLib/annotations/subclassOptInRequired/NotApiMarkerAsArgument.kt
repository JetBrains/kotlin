annotation class DummyAnnotation

@RequiresOptIn
annotation class NotDummyAnnotation

<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>@SubclassOptInRequired(DummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarkerA

<!OPT_IN_ARGUMENT_IS_NOT_MARKER("DummyAnnotation")!>@SubclassOptInRequired(DummyAnnotation::class, NotDummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarkerB
