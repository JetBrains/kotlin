// LANGUAGE: +MultiPlatformProjects
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry

data class X(val a: Int, val b: Int)

val (<caret>a, b) = X(1, 2)
