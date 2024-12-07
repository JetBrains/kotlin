// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: a/VeryBase.java
package a;

class VeryBase {
    public String foo = "OK";
}

// FILE: a/Base.java
package a;

public class Base extends VeryBase {
}

// FILE: b/Intermediate.java
package b;

class Intermediate extends a.Base {
}

// FILE: box.kt
package b

private class Final : Intermediate() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>foo<!> = "FAIL"
}

fun box(): String =
    Final().<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>foo<!>
