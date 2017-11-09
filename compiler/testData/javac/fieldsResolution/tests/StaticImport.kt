// FILE: a/x.java
package a;

public class x {

    public static final int I = 42;
    public static final int I1 = 42;
    public static final int I2 = I + I1;

}

// FILE: b/y.java
package b;

import static a.x.I;

public class y {
    public static final int O = I;
}

// FILE: b/z.java
package b;

import static a.x.*;

public class z {
    public static final int CONST = I + I1 + I2;
}