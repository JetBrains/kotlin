// FIX: Remove explicit type specification from 'bar'
// WITH_RUNTIME
fun bar(): MutableList<String> = mutableListOf<String<caret>>()