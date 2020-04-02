// FOREIGN_ANNOTATIONS

// FILE: test/package-info.java

@javax.annotation.ParametersAreNonnullByDefault()
package test;

// FILE: test/A.java

package test;

import javax.annotation.*;

public class A {
    @Nullable public String field = null;

    public String foo(String q, @Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }
}

// FILE: test2/A2.java

package test2;

import javax.annotation.*;

public class A2 {
    @Nullable public String field = null;

    public String foo(String q, @Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }
}

