package test;

import java.util.*;

public class A<T> {
    A<List<T>> bar(List<Map<String, Integer>> x) { return null; }

    static A rawField = null;
}
