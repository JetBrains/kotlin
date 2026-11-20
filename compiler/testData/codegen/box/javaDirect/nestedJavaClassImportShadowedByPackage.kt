// TARGET_BACKEND: JVM
// ISSUE: KT-90357
// USE_PSI_JAVA_FACADE: false
// The PSI Java facade resolves `import shp.Marker.Mode` to the class `Mode` of the package `shp.Marker` (KT-90484)

// MODULE: decoy
// FILE: shp/Marker/Mode.kt
package shp.Marker

enum class Mode { VALUE, DECOY }

// MODULE: main(decoy)
// FILE: shp/Marker.java
package shp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Marker {
    Mode value();

    enum Mode { FIRST, VALUE }
}

// FILE: shp/Greeter.java
package shp;

import shp.Marker.Mode;

public interface Greeter {
    @Marker(Mode.VALUE)
    String greet(String who);
}

// FILE: shp/TypeUser.java
package shp;

import shp.Marker.Mode;

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

    val annotationValue = runCatching {
        Delegator::class.java.getDeclaredMethod("greet", String::class.java)
            .getAnnotation(Marker::class.java).value
    }
    val returnValue = runCatching { TypeUser.mode().name }
    if (annotationValue.getOrNull() != Marker.Mode.VALUE) return "FAIL: wrong annotation value"
    if (returnValue.getOrNull() != "VALUE") return "FAIL: wrong return type"

    return "OK"
}
