// "class org.jetbrains.kotlin.idea.quickfix.AddGenericUpperBoundFix" "false"
// ERROR: Type argument is not within its bounds: should be subtype of 'Any'

fun <T : Any> foo() = 1

fun <E : Any?> bar() = foo<E<caret>>()
