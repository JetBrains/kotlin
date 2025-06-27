// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/A.java
package test;

public interface A<A1, A2> {}

// FILE: test/AImpl.java
package test;

public interface AImpl extends A {}

// FILE: test/B.java
package test;

public interface B<B1 extends Number, B2> extends A<B2, B1> {}

// FILE: test/BImpl.java
package test;

public interface BImpl extends B {}

// FILE: test/C.java
package test;

import java.io.Serializable;

public interface C<C1 extends Comparable<String> & CharSequence & Serializable, C2 extends Number & Serializable> {}

// FILE: test/CImpl.java
package test;

public interface CImpl extends C {}

// FILE: box.kt
package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

class TestA : AImpl
class TestB : BImpl
class TestC : CImpl

fun box(): String {
    assertEquals(
        "[test.AImpl, test.A<(raw) kotlin.Any!, (raw) kotlin.Any!>, kotlin.Any]",
        TestA::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.BImpl, test.B<(raw) kotlin.Number!, (raw) kotlin.Any!>, test.A<kotlin.Any!, kotlin.Number!>, kotlin.Any]",
        TestB::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.CImpl, test.C<(raw) kotlin.Comparable<*>!, (raw) kotlin.Number!>, kotlin.Any]",
        TestC::class.allSupertypes.toString(),
    )

    return "OK"
}
