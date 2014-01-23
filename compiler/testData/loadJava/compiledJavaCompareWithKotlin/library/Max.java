package test;

import java.util.Collection;

public class Max {
    public <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
        throw new UnsupportedOperationException();
    }
}
