// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NO_VALUE_FOR_PARAMETER
// FILE: A.java

/**
 * @deprecated
 */
public class A {
    /**
     * @deprecated
     */
    public String getFoo(String text) {
        return text;
    }
}

// FILE: B.kt

class B(private val foo: String) : <!DEPRECATION!>A<!>() {
    override fun getFoo(text: String): String = super.<!DEPRECATION!>getFoo<!>(text + foo)
}
