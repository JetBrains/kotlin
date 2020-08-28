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

class B(private <!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>@property:Deprecated<!> val foo: String) : A() {
    override fun getFoo(text: String): String = super.getFoo(text + foo)
}
