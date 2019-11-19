// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// TARGET_BACKEND: JVM

@Target(AnnotationTarget.FIELD)
annotation class Ann

class C {
    @Ann
    lateinit var x0: String
}

fun box(): String {
    require(C::class.java.getDeclaredField("x0")?.getAnnotation(Ann::class.java) != null)
    return "OK"
}
