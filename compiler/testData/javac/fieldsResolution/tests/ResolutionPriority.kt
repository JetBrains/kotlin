// FILE: a/x.java
package a;

public class x {
    public static final int I = 42;
}

// FILE: a/y.java
package a;

public class y extends x {

    public static final int I = x.I * 2;

    public class Inner {
        public static final int Y = I;
    }

    public class Inner2 extends x {
        public static final int Y = I;
    }

}

// FILE: b/b.java
package b;

public class b {
    public static final int I = 84;
}

// FILE: c/c.java
package c;

import static a.x.I;
import static b.b.*;

public class c extends a.x {

    public static final int O = I;

    public class Inner {
        public static final int O = I;
    }

}

// FILE: c/e.java
package c;

import static a.x.I;

public class e extends a.x {

    public static final int O = I;

    public class Inner {
        public static final int O = I;
    }

}

// FILE: c/d.java
package c;

import static b.b.*;

public class d extends a.x {

    public static final int O = I;

    public class Inner {
        public static final int O = I;
    }

}