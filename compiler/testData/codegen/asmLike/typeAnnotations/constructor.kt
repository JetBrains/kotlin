// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TYPE_ANNOTATIONS
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class Kotlin(s: @TypeAnn("1") String, p: @TypeAnn("123") String) {

    private constructor(s: @TypeAnn("private") String) : this("1", "2")

    fun foo() {
        { Kotlin("123") }()
    }
}
