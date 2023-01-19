// test.AKotlin
// SKIP_IDE_TEST

// DISABLE_SEALED_INHERITOR_CALCULATOR

// FILE: AKotlin.kt
package test
import test.BJava.FOO

open class AKotlin

// FILE: test/BJava.java
package test;

public class BJava extends AKotlin {
    public final static String FOO = "foo";
}

// FILE: CKotlin.kt
package test

class CKotlin: BJava()