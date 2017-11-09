package foo;

import javax.annotation.*;

@FieldsAreNullable
public class A {
    public String field = null;
    @Nonnull
    public String nonNullField = "";

    public String foo(String q, @Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }
}
