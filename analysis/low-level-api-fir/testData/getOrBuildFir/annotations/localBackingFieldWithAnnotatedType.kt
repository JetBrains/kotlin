// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry
// LANGUAGE: +ExplicitBackingFields
// ISSUE: KT-83754

fun usage() {
    open class A {
        val prop: Any
            field: <expr>@Ann("str")</expr> Int = 1
    }
}

@Target(AnnotationTarget.TYPE)
annotation class Ann(val s: String)
