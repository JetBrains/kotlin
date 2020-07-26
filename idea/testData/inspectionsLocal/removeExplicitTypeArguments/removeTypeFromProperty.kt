// FIX: Remove explicit type specification from 'foo'
// WITH_RUNTIME
val foo: MutableList<String> = mutableListOf<String<caret>>()