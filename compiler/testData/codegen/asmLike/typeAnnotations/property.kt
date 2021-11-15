// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// WITH_REFLECT
// FULL_JDK
package foo

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeAnnSource

class Kotlin {

    val valProp: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String = "123"

    var varProp: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String = "123"

    var customSetter: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String = "123"
        set(field: String) {}

    @JvmField
    var jvmField: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String = "123"

    lateinit var lateinitProp: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String

    val annotatedGetter: Int
        get(): @TypeAnn("1") @TypeAnnBinary @TypeAnnSource  Int = 123

    val unannotatedGetter: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource Int
        get(): Int = 123

    companion object {
        var companionVarProperty: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String = "123"

        @JvmStatic
        var jvmStatic: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource String = "123"
    }
}
