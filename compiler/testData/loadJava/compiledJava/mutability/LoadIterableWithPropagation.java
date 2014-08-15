package test;

import org.jetbrains.annotations.*;
import java.util.ArrayList;

public interface LoadIterableWithPropagation {
    public interface LoadIterable<T> {
        public @Mutable Iterable<T> getIterable();
        public void setIterable(@Mutable Iterable<T> Iterable);

        public @ReadOnly Iterable<T> getReadOnlyIterable();
        public void setReadOnlyIterable(@ReadOnly Iterable<T> Iterable);
    }

    public class LoadIterableImpl<T> implements LoadIterable<T> {
        public Iterable<T> getIterable() {return new ArrayList<T>();}
        public void setIterable(Iterable<T> Iterable) {}

        public Iterable<T> getReadOnlyIterable() {return new ArrayList<T>();}
        public void setReadOnlyIterable(Iterable<T> Iterable) {}
    }
}
