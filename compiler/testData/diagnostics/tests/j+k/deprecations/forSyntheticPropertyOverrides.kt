// FIR_IDENTICAL
// LANGUAGE: -StopPropagatingDeprecationThroughOverrides
// FILE: JavaClass.java

public class JavaClass {
    public String getFoo() { return ""; }
    public String getBar() { return ""; }
}

// FILE: main.kt

open class KotlinClass : JavaClass() {
    @Deprecated("sdas")
    override fun getFoo(): String {
        return super.getFoo()
    }

    @Deprecated("dasd", level = DeprecationLevel.ERROR)
    override fun getBar(): String {
        return super.getBar()
    }
}

class KotlinSubClass : KotlinClass() {
    override fun <!OVERRIDE_DEPRECATION!>getFoo<!>(): String {
        return super.<!DEPRECATION!>getFoo<!>()
    }

    override fun <!OVERRIDE_DEPRECATION!>getBar<!>(): String {
        return super.<!DEPRECATION_ERROR!>getBar<!>()
    }
}

fun main(kotlinClass: KotlinClass, kotlinSubClass: KotlinSubClass) {
    kotlinClass.<!DEPRECATION!>foo<!>
    kotlinClass.<!DEPRECATION_ERROR!>bar<!>

    kotlinSubClass.foo
    kotlinSubClass.bar
}