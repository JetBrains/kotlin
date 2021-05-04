// IGNORE_BACKEND: JVM
// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target( AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn()

interface Simple

class Kotlin {

    var <@TypeParameterAnn T: @TypeAnn("Simple") Simple> T.z: T?
        get() = null
        set(value) {}
}
