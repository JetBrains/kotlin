// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class MyAnn(val cls: KClass<*>)

val s: @MyAnn(Array<String>::class) String = ""

fun box(): String {
    val ann = ::s.returnType.annotations[0] as MyAnn
    return if (ann.cls == Array<String>::class) "OK" else "Fail: ${ann.cls}"
}
