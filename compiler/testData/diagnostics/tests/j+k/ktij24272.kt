// FIR_IDENTICAL
// FILE: use.kt
package one

fun getStructureElementFor() {
    val container: SuperJava = if (true) {
        true <!CAST_NEVER_SUCCEEDS!>as<!> Child2
    } else {
        false <!CAST_NEVER_SUCCEEDS!>as<!> Child1
    }
}

// FILE: SuperJava.java
package one;

public class SuperJava {
}

// FILE: Child1.java
package one;

public class Child1 extends SuperJava implements Cloneable {
}

// FILE: Child2.java
package one;

public class Child2 extends SuperJava implements Cloneable {
}
