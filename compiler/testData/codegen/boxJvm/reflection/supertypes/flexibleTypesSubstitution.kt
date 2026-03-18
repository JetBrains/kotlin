// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/A.java
package test;

public interface A<A1, A2> {}

// FILE: test/B.java
package test;

public interface B<B1, B2> extends A<B2, B1> {}

// FILE: test/Test1.java
package test;

public class Test1<T> implements B<Object, T> {}

// FILE: test/Test2.java
package test;

import java.util.List;

public class Test2<U> implements B<U, List<U>> {}

// FILE: box.kt
package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

class Test3<V> : B<V?, V & Any>

fun box(): String {
    assertEquals(
        "[test.B<kotlin.Any!, T!>, test.A<T!, kotlin.Any!>, kotlin.Any]",
        Test1::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.B<U!, kotlin.collections.(Mutable)List<U!>!>, test.A<kotlin.collections.(Mutable)List<U!>!, U!>, kotlin.Any]",
        Test2::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.B<V?, V & Any>, test.A<(V & Any..V?), V?>, kotlin.Any]",
        Test3::class.allSupertypes.toString(),
    )

    return "OK"
}
