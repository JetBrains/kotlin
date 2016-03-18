package test;

import java.util.*;

public class MethodWithMappedClasses {
    public <T> void copy(List<? super T> dest, List<T> src) {
        throw new UnsupportedOperationException();
    }

    public <T> void copyMap(Map<String, ? super T> dest, Map<String, T> src) {
        throw new UnsupportedOperationException();
    }
}
