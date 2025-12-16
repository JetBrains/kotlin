// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// Disable K1 since it reports: NOTHING_TO_OVERRIDE: 'propertyEraseGenericInJava' overrides nothing
// IGNORE_BACKEND_K1: ANY

// FILE: main.kt
open class Base {
    open fun integer(a: Int) = Unit
    open fun nullableInteger(a: Int) = Unit
    open fun character(a: Char) = Unit
    open fun <T> eraseGenericInJava(t: T) = Unit
    open var propertyGetterCovariantOverrideInJava: Number = 1
    open val <T> T.propertyEraseGenericInJava: Any get() = Unit
}

class FinalWithOverride : Middle() {
    override fun integer(a: Int) = Unit
    override fun nullableInteger(a: Int?) = Unit
    override fun character(a: Char) = Unit
    override fun eraseGenericInJava(t: Any) = Unit

    override var propertyGetterCovariantOverrideInJava: Number = 1
    override fun getPropertyGetterCovariantOverrideInJava(): Int? = 1

    override val Any.propertyEraseGenericInJava: Any get() = Unit
}

class Final : Base()

// FILE: Middle.java
public class Middle extends Base {
    public void integer(Integer x) {}
    public void nullableInteger(Integer a) {}
    public void character(Character c) {}
    @Override public void eraseGenericInJava(Object t) {}
    @Override public Integer getPropertyGetterCovariantOverrideInJava() {return 1;}
    @Override public Object getPropertyEraseGenericInJava(Object $this$prop) {return null;}
}
