package org.jetbrains.eval4j.test;

import java.lang.reflect.Array;

public class ArrayHelper {
    public static Object newMultiArray(Class<?> elementType, Integer... dimensions) {
        int[] dims = new int[dimensions.length];
        int i = 0;
        for (Integer dimension : dimensions) {
            dims[i] = dimension;
            i++;
        }

        return Array.newInstance(elementType, dims);
    }
}
