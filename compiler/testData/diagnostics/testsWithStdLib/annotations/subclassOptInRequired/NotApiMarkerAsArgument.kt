// FIR_IDENTICAL

annotation class DummyAnnotation

<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>@SubclassOptInRequired(DummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarker
