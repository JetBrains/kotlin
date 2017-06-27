// FILE: a/x.java
package a;

public class x {
    class O {}
}

// FILE: a/x1.java
package a;

public class x1 extends x {}

// FILE: a/x2.java
package a;

public class x2 extends x1 {}

// FILE: a/i.java
package a;

public interface i {
    public class O {}
}

// FILE: a/i2.java
package a;

public interface i2 extends i {
    public class O {}
}

// FILE: a/i3.java
package a;

public interface i3 extends i2 {}

// FILE: b/test.java
package b;

import a.*;

public class test extends x2 implements i3 {
    public O getO() { return null; }
}

// FILE: test.kt
package b

fun test1() = test().getO()