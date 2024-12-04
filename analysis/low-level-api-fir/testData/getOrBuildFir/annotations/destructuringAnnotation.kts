// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

data class MyPair(val a: Int, val b: Int)
const val prop = 0
annotation class DestrAnno(val s: String)

<expr>@DestrAnno("destr 1 $prop")</expr>
val (a, b) = MyPair(1, 2)
