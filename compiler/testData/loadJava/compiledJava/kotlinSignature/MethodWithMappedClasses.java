package test;

import java.util.*;

public class MethodWithMappedClasses {
    public <T> void copy(List<? super T> dest, List<T> src) {
        throw new UnsupportedOperationException();
    }
}
