// FILE: a/x.java
package a;

public class x {
    public static final int I = 42;
}

// FILE: a/y.java
package a;

public class y {
    public static final int I = 42;
}

// FILE: b/t.java
package b;

import static a.x.I;
import static a.y.I;

public class t {
    public static final int CONST = I;
}

// FILE: b/t1.java
package b;

import static a.x.I;
import static a.x.I;

public class t1 {
    public static final int CONST = I;
}