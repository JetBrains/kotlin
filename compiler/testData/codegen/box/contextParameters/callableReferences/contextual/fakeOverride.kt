// LANGUAGE: +ContextParameters +CallableReferencesToContextual

open class Base {
    context(c: String)
    open fun f(): String = "base-$c"

    context(c: String)
    open val p: String
        get() = "basep-$c"
}

class Derived : Base() {
    context(c: String)
    override fun f(): String = "derived-$c"
}

fun box(): String = context("ctx") {
    val viaBase: (Base) -> String = Base::f
    if (viaBase(Derived()) != "derived-ctx") return@context "FAIL 1: ${viaBase(Derived())}"
    if (viaBase(Base()) != "base-ctx") return@context "FAIL 2: ${viaBase(Base())}"

    val bound: () -> String = Derived()::f
    if (bound() != "derived-ctx") return@context "FAIL 3: ${bound()}"

    val prop: (Derived) -> String = Derived::p
    if (prop(Derived()) != "basep-ctx") return@context "FAIL 4: ${prop(Derived())}"

    "OK"
}
