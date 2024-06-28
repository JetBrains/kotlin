// LANGUAGE: -StopPropagatingDeprecationThroughOverrides
// FILE: JavaClass.java

public class JavaClass {
    @Deprecated
    public String getFoo() { return ""; }
    public void setFoo(String x) {}
}

// FILE: main.kt

open class KotlinClass : JavaClass() {
    @Deprecated("")
    override fun getFoo(): String {
        return super.<!DEPRECATION!>getFoo<!>()
    }

    override fun setFoo(x: String?) {
        super.setFoo(x)
    }
}

class KotlinSubClass : KotlinClass() {
    override fun <!OVERRIDE_DEPRECATION!>getFoo<!>(): String {
        return super.<!DEPRECATION!>getFoo<!>()
    }

    override fun setFoo(x: String?) {
        super.setFoo(x)
    }
}

fun main(j: JavaClass, k: KotlinClass, ks: KotlinSubClass) {
    j.<!DEPRECATION!>getFoo<!>()
    j.setFoo("")

    j.<!DEPRECATION!>foo<!>
    j.foo = ""

    k.<!DEPRECATION!>getFoo<!>()
    k.setFoo("")

    k.<!DEPRECATION!>foo<!>
    k.foo = ""

    ks.getFoo()
    ks.setFoo("")

    ks.foo
    ks.foo = ""
}