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
import java.util.List;

public interface C<
        C1 extends Comparable<String> & CharSequence & Serializable,
        C2 extends Number & Serializable,
        C3 extends List<String[]>
> {}

// FILE: test/CImpl.java
package test;

public interface CImpl extends C {}

// FILE: test/D.java
package test;

import java.util.List;

public interface D<D1 extends Number, D2 extends D3, D3 extends D1, D4 extends D3> {}

// FILE: test/DImpl.java
package test;

public interface DImpl extends D {}

// FILE: test/E.java
package test;

public interface E<E1 extends E, E2 extends E1> {}

// FILE: test/EImpl.java
package test;

public interface EImpl extends E {}

// FILE: test/F.java
package test;

public class F<F1 extends Number> {
    public class Inner<F2 extends F1> {}
}

// FILE: test/FImpl.java
package test;

public class FImpl extends F {
    public class InnerImpl extends F.Inner {}
}

// FILE: test/G.java
package test;

public class G<G1> {
    public class Inner {}
}

// FILE: test/GImpl.java
package test;

public class GImpl extends G {
    public class InnerImpl extends G.Inner {}
}

// FILE: box.kt
package test

import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

class TestA : AImpl
class TestB : BImpl
class TestC : CImpl
class TestD : DImpl
class TestE : EImpl

class TestF : FImpl() {
    inner class TestInner : FImpl.InnerImpl()
}

class TestG : GImpl() {
    inner class TestInner : GImpl.InnerImpl()
}

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
        "[test.CImpl, test.C<(raw) kotlin.Comparable<*>!, (raw) kotlin.Number!, (raw) kotlin.collections.(Mutable)List<*>!>, kotlin.Any]",
        TestC::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.DImpl, test.D<(raw) kotlin.Number!, (raw) kotlin.Number!, (raw) kotlin.Number!, (raw) kotlin.Number!>, kotlin.Any]",
        TestD::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.EImpl, test.E<(raw) test.E<*, *>!, (raw) test.E<*, *>!>, kotlin.Any]",
        TestE::class.allSupertypes.toString(),
    )

    assertEquals(
        "[test.FImpl.InnerImpl, test.F<(raw) kotlin.Number!>.Inner<(raw) kotlin.Number!>, kotlin.Any]",
        TestF.TestInner::class.allSupertypes.toString(),
    )

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals("[test.GImpl.InnerImpl, test.G<kotlin.Any!>.Inner, kotlin.Any]", TestG.TestInner::class.allSupertypes.toString())
    } else {
        // The new implementation seems more correct here, but in reality it's unlikely to affect anything.
        assertEquals("[test.GImpl.InnerImpl, test.G<(raw) kotlin.Any!>.Inner, kotlin.Any]", TestG.TestInner::class.allSupertypes.toString())
    }

    return "OK"
}
