// TARGET_BACKEND: JVM
// FULL_JDK
// DUMP_IR
// ISSUE: KT-68401
// FILE: other/AClass.java
package other;
public class AClass extends PrivateSuper {}

// FILE: other/BClass.java
package other;
public class BClass extends PrivateSuper {}

// FILE: other/PrivateSuper.java
package other;
class PrivateSuper extends PublicSuper {}

// FILE: other/PublicSuper.java
package other;
public class PublicSuper {}

// FILE: box.kt
package foo

import other.AClass
import other.BClass
import other.PublicSuper
import java.lang.IllegalAccessError

var temp: PublicSuper? = null

fun <T> select(vararg t: T): T = t[0]

fun box(): String {
    temp = if ("true" == "false") AClass() else BClass()

    temp = when {
        "true" == "false" -> AClass()
        else -> BClass()
    }

    try {
        temp = select(AClass(), BClass())
        return "FAIL"
    } catch (e: IllegalAccessError) {
        // error is expected
    }

    return "OK"
}