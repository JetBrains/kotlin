import kotlin.properties.ReadOnlyProperty;
import org.jetbrains.annotations.NotNull;

class J {

    public static class Foo<T> implements ReadOnlyProperty<A<T>, B> {
        public Foo(T t, @NotNull String s) {
        }
    }
}