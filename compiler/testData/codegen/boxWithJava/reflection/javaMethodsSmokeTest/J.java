import java.util.List;

public class J {
    void simple() {}

    void objectTypes(
            Object o, String s, Object[] oo, String[] ss
    ) {}

    void primitives(
            boolean z, char c, byte b, short s, int i, float f, long j, double d
    ) {}

    void primitiveArrays(
            boolean[] z, char[] c, byte[] b, short[] s, int[] i, float[] f, long[] j, double[] d
    ) {}

    void multiDimensionalArrays(
            int[][][] i, Cloneable[][][][] c
    ) {}

    void wildcards(
            List<? extends Number> l, List<? super Cloneable> m
    ) {}
}
