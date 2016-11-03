// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.test.assertEquals

fun box(): String {
    val ctor = Retention::class.constructors.single()
    val r = ctor.callBy(mapOf(
            ctor.parameters.single { it.name == "value" } to RetentionPolicy.RUNTIME
    ))
    assertEquals(RetentionPolicy.RUNTIME, r.value as RetentionPolicy)
    assertEquals(Retention::class.java.classLoader, r.javaClass.classLoader)
    return "OK"
}
