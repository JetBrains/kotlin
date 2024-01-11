// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

annotation class DummyAnnotation

<!OPT_IN_ARGUMENT_IS_NOT_MARKER!>@SubclassOptInRequired(DummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarker
