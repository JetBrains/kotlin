// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ExplicitBackingFields
// ISSUE: KT-83754

fun usage() {
    open class A {
        val prop: Any
            field: @Ann("str") Int = 1
    }
}

@Target(AnnotationTarget.TYPE)
annotation class Ann(val s: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, explicitBackingField, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
