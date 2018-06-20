// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

open class A {
    @JvmField public val publicField = "1";
    @JvmField internal val internalField = "2";
    @JvmField protected val protectedfield = "3";
}

open class B : A() {

}

open class C : B() {
    fun test(): String {
        return super.publicField + super.internalField + super.protectedfield
    }
}


fun box(): String {
    return if (C().test() == "123") return "OK" else "fail"
}
