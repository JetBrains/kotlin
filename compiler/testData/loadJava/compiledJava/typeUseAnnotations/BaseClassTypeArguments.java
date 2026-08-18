// FULL_JDK
// FILE: test/I1.java
package test;

interface I1<T> {}

// FILE: test/I2.java
package test;

interface I2<T, K> {}

// FILE: test/I3.java
package test;

interface I3<T, K, L> {}

// FILE: test/A1.java
package test;

class A1<T> {}

// FILE: test/A2.java
package test;

class A2<T, K> {}

// FILE: test/A3.java
package test;

class A3<T, K, L> {}

// FILE: test/BaseClassTypeArguments.java
package test;

import org.jetbrains.annotations.*;

public class BaseClassTypeArguments<B> extends A3<@NotNull B [][][][][], I1<I1<@NotNull int @NotNull [][]>>, A2<B, int [] [] @NotNull []>> implements I1<@NotNull Integer @NotNull [][][]>, I2<@NotNull B, B>, I3<@NotNull B [][][][][], B, @NotNull B> {
    class Basic1 implements I1<@NotNull String> { }
    class Basic2 extends A1<@NotNull String> { }

    class ImplementedInterfacesTypeArguments<B> implements I1<I2<I1<@NotNull Integer @NotNull [][][]>, I1<@NotNull int [] @NotNull []>>>, I2<@NotNull B, B>, I3<@NotNull B [][][][][], I1<I1<@NotNull int @NotNull [][]>>, I2<B, int [] [] @NotNull []>> {
        public class BaseClassTypeArguments1<B> extends A3<@NotNull B [][][][][], I1<I1<@NotNull int @NotNull [][]>>, A2<B, int [] [] @NotNull []>> { }
    }
    static class BaseClassTypeArguments2<B> extends A3<@NotNull B [][][][][], I1<I1<@NotNull int @NotNull [][]>>, A2<B, int [] [] @NotNull []>> { }
}
