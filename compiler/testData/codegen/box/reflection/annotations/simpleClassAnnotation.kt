// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

@Retention(AnnotationRetention.RUNTIME)
annotation class Simple(val value: String)

@Simple("OK")
class A

fun box(): String {
    return (A::class.annotations.single() as Simple).value
}
