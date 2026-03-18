// TARGET_BACKEND: JVM
// FILE: emptyVarargOfBoxedPrimitiveType.kt

fun box(): String {
    takesVarargOfInt(1)
    takesVarargOfT(1)

    J.takesVarargOfInt(1)
    J.takesVarargOfInteger(1)
    J.takesVarargOfT(1)

    takesVarargOfInt(1, 2)
    takesVarargOfT(1, 2)

    J.takesVarargOfInt(1, 2)
    J.takesVarargOfInteger(1, 2)
    J.takesVarargOfT(1, 2)

    return "OK"
}

fun takesVarargOfInt(x: Int, vararg xs: Int) {}
fun <T> takesVarargOfT(x: T, vararg xs: T) {}

// FILE: J.java
public class J {
    public static void takesVarargOfInt(int x1, int... xs) {}

    public static void takesVarargOfInteger(Integer x1, Integer... xs) {}

    public static <T> void takesVarargOfT(T x1, T... xs) {
        Class<?> x1Class = x1.getClass();
        Class<?> xsClass = xs.getClass();
        Class<?> xsComponentClass = xsClass.getComponentType();
        if (!xsComponentClass.equals(x1Class)) {
            throw new AssertionError(
                    "Wrong array component type " + xsComponentClass +
                            " for array class " + xsClass +
                            ", expected: " + x1Class
            );
        }
    }
}