// FILE: Test.java
public class Test {
    public static int i1 = 1;
    public static final int i2 = 1;
    public static final int i3 = i1;
    public static final int i4 = i2;
    public static int i5 = i1;
    public static int i6 = i2;

    public final int i7 = 1;
}

// FILE: a.kt
annotation class Ann(vararg val i: Int)

@Ann(
        Test.i1,
        Test.i2,
        Test.i3,
        Test.i4,
        Test.i5,
        Test.i6,
        Test().i7
)
class A
