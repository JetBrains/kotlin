@file:OptIn(ExperimentalSubclassOptIn::class)

annotation class DummyAnnotation

<!SUBCLASS_OPT_ARGUMENT_IS_NOT_MARKER!>@SubclassOptInRequired(DummyAnnotation::class)<!>
open class IncorrectSubclassOptInArgumentMarker
