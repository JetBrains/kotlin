// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/A.java
package test;

public interface A<A1, A2> {}

// FILE: test/B.java
package test;

public interface B<B1, B2> extends A<B2, B1> {}

// FILE: test/C.java
package test;

public class C<T> implements B<Object, T> {}

// FILE: test/D.java
package test;

import java.util.List;

public class D<U> implements B<U, List<U>> {}

// FILE: box.kt
package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(
        "[test.B<kotlin.Any!, T!>, test.A<T!, kotlin.Any!>, kotlin.Any]",
        C::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.B<U!, kotlin.collections.(Mutable)List<U!>!>, test.A<kotlin.collections.(Mutable)List<U!>!, U!>, kotlin.Any]",
        D::class.allSupertypes.toString(),
    )

    return "OK"
}
