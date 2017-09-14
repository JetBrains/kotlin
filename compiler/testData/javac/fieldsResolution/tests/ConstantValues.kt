// FILE: a/x.java
package a;

public class x {

    public static final String STR = "STR";
    public static final String STR2 = "STR" + STR;
    public static final int I = (15 / 5) + 3;
    public static final int IMAX = Integer.MAX_VALUE;
    public static final int I2 = Integer.MIN_VALUE + x.IMAX - 1;
    public static final double D = 25.0 - 15.0;
    public static final String NULL = null;
    public static final int VAL = 15 % 3;
    public static final boolean VAL1 = false;
    public static final boolean VAL2 = VAL1 && true;
    public static final boolean VAL3 = VAL1 || VAL2;
    public static final boolean VAL4 = I > I2;
    public static final boolean VAL5 = I < I2;
    public static final boolean VAL6 = I <= D;
    public static final boolean VAL7 = I >= D;
    public static final boolean VAL8 = VAL1 == VAL2;
    public static final boolean VAL9 = VAL1 != VAL2;
    public static final boolean VAL10 = !VAL9;
    public static final boolean VAL11 = STR instanceof String;
    public static final byte B = 10>>1;
    public static final byte B1 = 10<<1;
    public static final byte B2 = 1 & 0;
    public static final byte B3 = 1 | 0;
    public static final byte B4 = ~1;
    public static final byte B5 = 1 ^ 1;
    public static final byte B6 = B>>>1;

}

// FILE: test.kt
package a

val v1 = x.STR
val v2 = x.STR2
val v3 = x.I
val v4 = x.IMAX
val v5 = x.I2
val v6 = x.D
val v7 = x.NULL
val v8 = x.VAL