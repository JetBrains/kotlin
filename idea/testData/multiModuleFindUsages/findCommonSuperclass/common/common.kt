// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, expected

expect open class O<caret>Class(i: Int)
expect class Inheritor : OClass