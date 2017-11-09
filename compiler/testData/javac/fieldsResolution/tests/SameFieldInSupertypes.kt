// FILE: a/x.java
package a;

public class x {
    public static final int I = 42;
}

// FILE: a/y.java
package a;

public interface y {
    int I = 84;
}

// FILE: a/y2.java
package a;

public class y2 implements y {
    public static final int I = 168;
}

// FILE: a/z.java
package a;

public class z extends x implements y {}

// FILE: a/z1.java
package a;

public class z1 extends y2 implements y {}

// FILE: a/a.java
package a;

public class a {
    public static final int I = z.I;
    public static final int I2 = z1.I;
}