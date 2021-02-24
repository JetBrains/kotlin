import kotlin.reflect.KProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class D {
    private String _value = "";

    String getValue(@Nullable Object thisRef, @NotNull KProperty<?> property) {
        return _value;
    }

    void setValue(@Nullable Object thisRef, @NotNull KProperty<?> property, @NotNull String value) {
        _value = value;
    }
}