// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: K2 uses "$" as a separator for inner local class instead of "." as in K1. See `FirJvmSerializerExtension.getLocalClassIdOracle`.

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class Anno(val klass: KClass<*>)

fun box(): String {
    class A {
        inner class B : @Anno(OK::class) Any()
        inner class OK
    }

    val anno = A.B::class.supertypes.single().annotations.single() as Anno
    return anno.klass.java.simpleName
}
