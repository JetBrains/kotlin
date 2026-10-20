// TARGET_BACKEND: JVM
// USE_PSI_JAVA_FACADE: false
// The PSI Java facade resolves `import static p.X.Y.C` to `Y` of the package `p.X` (KT-90484)
// The class `p.X` clashes with the package `p.X` of another module. In the qualifier of a constant reference, `p.X` is
// the class (JLS 6.5.4.2), so `p.X.Y.C` refers to the nested object `p.X.Y`, as with javac, never to `Y` of the package.

// MODULE: decoy
// FILE: p/X/Y.kt
package p.X

object Y {
    const val C = "decoy"
}

// MODULE: main(decoy)
// FILE: p/X.kt
package p

class X {
    object Y {
        const val C = "nested"
    }
}

// FILE: u/Holder.java
package u;

import p.X;

import static p.X.Y.C;

public class Holder {
    public static final String QUALIFIED = p.X.Y.C;
    public static final String IMPORTED = X.Y.C;
    public static final String STATIC_IMPORTED = C;
}

// FILE: main.kt
import u.Holder

const val QUALIFIED = Holder.QUALIFIED
const val IMPORTED = Holder.IMPORTED
const val STATIC_IMPORTED = Holder.STATIC_IMPORTED

fun box(): String {
    for ((name, value) in listOf("QUALIFIED" to QUALIFIED, "IMPORTED" to IMPORTED, "STATIC_IMPORTED" to STATIC_IMPORTED)) {
        val javacValue = Holder::class.java.getField(name).get(null)
        if (javacValue != "nested") return "FAIL: javac $name=$javacValue"
        if (value != "nested") return "FAIL: $name=$value"
    }
    return "OK"
}
