// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: JavaClass.java

import static KotlinClass.*;

@Deprecated
public class JavaClass {
}

// FILE: main.kt
open class KotlinClass {
    fun foo(): <expr>JavaClass?</expr> = null
}
