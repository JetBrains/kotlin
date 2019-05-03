// FOREIGN_ANNOTATIONS

// FILE: A.java

import javax.annotation.*;

@ParametersAreNonnullByDefault
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

