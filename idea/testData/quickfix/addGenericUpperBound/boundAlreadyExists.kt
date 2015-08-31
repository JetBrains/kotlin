// "class org.jetbrains.kotlin.idea.quickfix.AddGenericUpperBoundFix" "false"
// ERROR: <html>Type argument is not within its bounds.<table><tr><td>Expected:</td><td>kotlin.Any</td></tr><tr><td>Found:</td><td>E</td></tr></table></html>

fun <T : Any> foo() = 1

fun <E : Any?> bar() = foo<E<caret>>()
