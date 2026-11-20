// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// The second entry misses its own val, so the declaration is a broken full form

data class X(val a: Int, val b: Int)

(val a, <caret>b) = X(1, 2)
