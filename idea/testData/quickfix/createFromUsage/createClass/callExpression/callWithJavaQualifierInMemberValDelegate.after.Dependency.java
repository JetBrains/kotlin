import kotlin.Deprecated;
import kotlin.PropertyMetadata;
import kotlin.properties.ReadOnlyProperty;
import org.jetbrains.annotations.NotNull;

class J {

    public static class Foo<T> implements ReadOnlyProperty<A<T>, B> {
        public Foo(T t, @NotNull String s) {
        }

        @NotNull
        @Deprecated
        public B get(@NotNull A<T> thisRef, @NotNull PropertyMetadata property) {
            return null;
        }

        @NotNull
        public B getValue(@NotNull A<T> thisRef, @NotNull PropertyMetadata property) {
            return null;
        }
    }
}