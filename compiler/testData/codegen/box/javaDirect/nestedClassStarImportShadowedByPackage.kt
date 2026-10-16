// TARGET_BACKEND: JVM
// ISSUE: KT-90357
// USE_PSI_JAVA_FACADE: false

// MODULE: decoy
// FILE: shp/Marker/Mode.kt
package shp.Marker

enum class Mode { VALUE, DECOY }

// MODULE: main(decoy)
// FILE: shp/Marker.kt
package shp

@Retention(AnnotationRetention.RUNTIME)
annotation class Marker(val value: Mode) {
    enum class Mode { FIRST, VALUE }
}

// FILE: shp/Greeter.java
package shp;

import shp.Marker.*;

public interface Greeter {
    @Marker(Mode.VALUE)
    String greet(String who);
}

// FILE: shp/TypeUser.java
package shp;

import shp.Marker.*;

public class TypeUser {
    public static Mode mode() {
        return Mode.VALUE;
    }
}

// FILE: main.kt
import shp.Greeter
import shp.Marker
import shp.TypeUser

class GreeterImpl : Greeter {
    override fun greet(who: String): String = "hi $who"
}

class Delegator(delegate: Greeter) : Greeter by delegate

fun box(): String {
    val delegated = Delegator(GreeterImpl())
    if (delegated.greet("Kotlin") != "hi Kotlin") return "FAIL: delegation"

    val annotation = Delegator::class.java.getDeclaredMethod("greet", String::class.java)
        .getAnnotation(Marker::class.java)
    if (annotation.value != Marker.Mode.VALUE) return "FAIL: wrong annotation value"
    if (TypeUser.mode().name != "VALUE") return "FAIL: wrong return type"

    return "OK"
}
