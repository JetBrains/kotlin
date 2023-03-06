// FIR_IDENTICAL
// !LANGUAGE: +AllowInternalInterfaceMembers

// MODULE: lib

internal interface I1 {
    internal val memberProperty: Int
    internal fun memberFunction(): Unit
}

interface I2 {
    internal val memberProperty: Int
    internal fun memberFunction(): Unit
}

interface I3 {
    val memberProperty: Int
    fun memberFunction(): Unit
}

interface I4 {
    internal val internalMember: Int
    val publicMember: Int
}

interface I5 {
    internal interface InternalInnerInterface
    internal class InternalInnerClass
}

class A : I1 {
    internal override val memberProperty: Int = 42
    internal override fun memberFunction() = Unit
}

class B : I2 {
    internal override val memberProperty: Int = 42
    internal override fun memberFunction() = Unit
}

class C : I1 {
    override val memberProperty: Int = 42
    override fun memberFunction() = Unit
}

class D : I2 {
    override val memberProperty: Int = 42
    override fun memberFunction() = Unit
}

class E : I3 {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!>  override val memberProperty: Int = 42
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!>  override fun memberFunction() = Unit
}

class F : I3 {
    override val memberProperty: Int = 42
    override fun memberFunction() = Unit
}

class G : I4 {
    internal override val internalMember: Int = 42
    override val publicMember: Int = 42
}

class H : I4 {
    override val internalMember: Int = 42
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!> override val publicMember: Int = 42
}

// MODULE: main(lib)()()

class I : I2 {
    internal <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> val memberProperty: Int = 42
    internal <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun memberFunction() = Unit
}

class J : I2 {
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> val memberProperty: Int = 42
    <!CANNOT_OVERRIDE_INVISIBLE_MEMBER!>override<!> fun memberFunction() = Unit
}

fun useI2(i2: I2) {
    i2.<!INVISIBLE_REFERENCE!>memberProperty<!>
    i2.<!INVISIBLE_REFERENCE!>memberFunction<!>()
}
