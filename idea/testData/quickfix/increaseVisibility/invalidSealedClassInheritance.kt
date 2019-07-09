// "Make '<init>' public" "false"
// "Make '<init>' internal" "false"
// ACTION: Introduce import alias
// ERROR: Cannot access '<init>': it is private in 'SealedClass'
// ERROR: This type is sealed, so it can be inherited by only its own nested classes or objects

sealed class SealedClass

fun test() {
    class Test : <caret>SealedClass()
}