// LANGUAGE: +AnnotationAllUseSiteTarget
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// ALLOW_AST_ACCESS
// PLATFORM_DEPENDANT_METADATA

package test

annotation class Default

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class Prop

@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class Function

data class MyRecord(@all:Default @all:Prop @all:Function val x: String)

object O {
    @all:Default @all:Prop @all:Function val x = 0
        get() = field

    @all:Default @all:Prop @all:Function var y = 0
        get() = field
        set(param) { field = param }
}
