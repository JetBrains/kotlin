// "Add 'noinline' to parameter 'block'" "true"

inline fun foo(block: () -> Unit) = block<caret>
