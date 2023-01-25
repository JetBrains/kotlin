// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

annotation class Ann

class X {
    <expr>@delegate:Ann</expr>
    val a by lazy { 1 }
}