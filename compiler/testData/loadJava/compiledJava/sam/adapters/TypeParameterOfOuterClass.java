package test;

import java.util.*;

public class TypeParameterOfOuterClass<T> {
    public class Inner {
        public void foo(Comparator<T> comparator) {
        }
    }
}
