// FOREIGN_ANNOTATIONS
import javax.annotation.*;

public class Simple {
    @Nullable public String field = null;

    @Nullable
    public String foo(@Nonnull String x, @CheckForNull CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }

}