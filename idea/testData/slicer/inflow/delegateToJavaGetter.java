import kotlin.reflect.KProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class D {
    public static D INSTANCE = new D();

    int getValue(@Nullable Object thisRef, @NotNull KProperty<?> property) {
        return 1;
    }
}