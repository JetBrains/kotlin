// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: base/Y.java
package base;

class Y {
    public String f = "OK";
}

// FILE: base/A.java
package base;

public class A extends Y {}

// FILE: B.kt
package base

open class B : A() {
    private val f = "FAIL"
}

// FILE: C.java
import base.B;

public class C extends B {}

// FILE: test.kt
fun box(): String {
    return C().<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>f<!>
}
