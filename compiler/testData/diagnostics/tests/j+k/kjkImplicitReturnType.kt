// FIR_IDENTICAL
// ISSUE: KT-66048

// FILE: Java1.java
public class Java1 extends KotlinClass  {
    @Override
    public String a() {
        return "2";
    }

    @Override
    public String getB() {
        return "2";
    }

    @Override
    public String getC() {
        return "2";
    }

    @Override
    public void setC(String value) {
    }

    @Override
    public String getD() {
        return "2";
    }

    @Override
    public void setD(String value) {
    }

    @Override
    public String getE() {
        return "2";
    }

    @Override
    public String getF() {
        return "2";
    }
}

// FILE: test.kt
open class KotlinClass {
    open fun a() = "1"
    open val b = "1"
    open var c = "1"
    open var d: String = "1"
    open var e = "1"
    open var f: String = "1"
}

class B : Java1() {
    override fun a(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.a()
    override val b: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.b
    override var c: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.c
    override var d: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.d
    override var e: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.e
    override var f: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = super.f
}
