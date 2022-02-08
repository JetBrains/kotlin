// KOTLIN_CONFIGURATION_FLAGS: -JVM.EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)


class Kotlin {

    fun foo(s: @TypeAnn("1") String) {
    }

    fun <T : @TypeAnn("Ant") Any> bar(p: T): T {
        return p
    }

}
