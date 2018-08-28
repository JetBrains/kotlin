// "Add 'inline' modifier" "true"
// ERROR: Only type parameters of inline functions can be reified

expect fun <reif<caret>ied T> inlineFun(t: T)