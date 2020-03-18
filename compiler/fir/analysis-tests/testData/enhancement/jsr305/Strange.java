// FOREIGN_ANNOTATIONS
import javax.annotation.*;
import javax.annotation.meta.*;

public class Strange {
    @Nonnull(when=When.UNKNOWN) public String field = null;

    @Nonnull(when=When.MAYBE)
    public String foo(@Nonnull(when=When.ALWAYS) String x, @Nonnull(when=When.NEVER) CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }

}