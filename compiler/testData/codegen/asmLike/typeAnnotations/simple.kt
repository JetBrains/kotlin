// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeAnnSource

class Kotlin {

    fun foo(s: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String) {
    }

    fun foo2(): @TypeAnn("2") @TypeAnnBinary @TypeAnnSource String {
        return "OK"
    }

    fun fooArray(s: Array<@TypeAnn("3") @TypeAnnBinary @TypeAnnSource String>) {
    }

    fun fooArray2(): Array<@TypeAnn("4") @TypeAnnBinary @TypeAnnSource String>? {
        return null
    }

}
