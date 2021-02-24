import org.jetbrains.annotations.*;

public class ValueParameter<T> {
    public interface A<T1, T2> {}

    public void foo1(A<@Nullable String, @Nullable T> x) { }
    public void foo2(A<@Nullable String, @NotNull T> x) { }
    public void foo3(A<@NotNull String, @NotNull T> x) { }
    public void foo4(@NotNull T [] x) { }
    public void foo41(@Nullable String x) { }
    public void foo411(T x) { }
    public void foo5(@Nullable String [] x) { }
}
