// TARGET_BACKEND: JVM
// WITH_REFLECT

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
