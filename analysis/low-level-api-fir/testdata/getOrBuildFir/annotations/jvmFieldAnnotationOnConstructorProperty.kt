// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
// WITH_STDLIB

class MyClass(
    <expr>@JvmField</expr>
    var addCommaWarning: Boolean = false
) : A() {

}

open class A