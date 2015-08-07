// !DIAGNOSTICS: -NO_VALUE_FOR_PARAMETER
// FILE: A.java

@Deprecated
public class A {
    @Deprecated
    public String getFoo(String text) {
        return text;
    }
}

// FILE: B.kt

class B(private @property:deprecated val foo: String) : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>A<!>() {
    override fun getFoo(text: String): String = super.<!DEPRECATED_SYMBOL_WITH_MESSAGE!>getFoo<!>(text + <!DEPRECATED_SYMBOL!>foo<!>)
}