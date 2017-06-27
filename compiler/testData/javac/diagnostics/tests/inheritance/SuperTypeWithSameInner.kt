// FILE: a/x.java
package a;

public class x {
    public class O {}
}

// FILE: a/i.java
package a;

public interface i {
    public class Z {}
}

// FILE: a/i2.java
package a;

public interface i2 extends i {
    public class Z {}
}

// FILE: a/test.java
package a;

public class test extends x implements i2 {
    public Z getZ() { return null; }
    public O getO() { return null; }
}

// FILE: test.kt
package a

fun test1() = test().getZ()
fun test2() = test().getO()