// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.valueParameters

@Retention(AnnotationRetention.RUNTIME)
annotation class Simple(val value: String)

fun local(): Any {
    class A(@Simple("OK") val z: String)
    return A("OK")
}

fun localCaptured(): Any {
    val z  = 1
    class A(@Simple("K") val z: String) {
        val x = z
    }
    return A("K")
}

fun box(): String {
    return (local()::class.constructors.single().valueParameters.single().annotations.single() as Simple).value
    //KT-25573
    //return (localCaptured()::class.constructors.single().valueParameters.single().annotations.single() as Simple).value
}
