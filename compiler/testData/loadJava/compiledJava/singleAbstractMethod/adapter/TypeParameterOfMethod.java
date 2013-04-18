package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.Comparator;

public class TypeParameterOfMethod {
    public static <T> T max(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }

    public static <T extends CharSequence> T max2(Comparator<T> comparator, T value1, T value2) {
        return comparator.compare(value1, value2) > 0 ? value1 : value2;
    }

    public static <A extends CharSequence, B extends List<A>> void method(Comparator<A> a, B b) {
        throw new UnsupportedOperationException();
    }
}
