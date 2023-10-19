// FILE: usage.kt
package first

import <!DEPRECATION!>third.JavaClass.NestedJavaClass<!>

// FILE: KotlinAnnotation.kt
package second

class KotlinClass {
    fun foo(i: Int) {}
    annotation class KotlinAnnotation
}

// FILE: third/JavaClass.java
package third;

import second.KotlinClass.*;

import static second.KotlinClass.*;

/**
 * @deprecated deprecated message
 */
@KotlinAnnotation
public class JavaClass {
    /**
     * @deprecated deprecated message
     */
    @KotlinAnnotation
    public static class NestedJavaClass {

    }
}
