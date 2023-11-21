// FIR_IDENTICAL
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
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Test.i1<!>,
        Test.i2,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Test.i3<!>,
        Test.i4,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Test.i5<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Test.i6<!>,
        <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Test().i7<!>
)
class A
