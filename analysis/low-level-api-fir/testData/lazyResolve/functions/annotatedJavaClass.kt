// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: main.kt
open class KotlinClass {
    fun f<caret>oo(): JavaClass? = null
}

// FILE: JavaClass.java
import static KotlinClass.KotlinClass;

@KotlinClass
public class JavaClass {
}
