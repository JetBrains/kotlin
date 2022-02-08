import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Java<T extends @Nullable Object> {
    @NonNull T getFoo();
}
