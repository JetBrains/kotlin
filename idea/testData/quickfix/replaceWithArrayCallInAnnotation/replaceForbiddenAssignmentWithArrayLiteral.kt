// "Replace with array call" "true"
// COMPILER_ARGUMENTS: -XXLanguage:+ProhibitAssigningSingleElementsToVarargsInNamedForm
// DISABLE-ERRORS

annotation class Some(vararg val strings: String)

@Some(strings = <caret>"value")
class My
