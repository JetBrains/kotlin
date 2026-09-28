// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
// LANGUAGE: +ExplicitBackingFields

class A {
    val prop: Any
        <expr>@Anno</expr> field: Int = 1
}

@Target(AnnotationTarget.FIELD)
annotation class Anno
