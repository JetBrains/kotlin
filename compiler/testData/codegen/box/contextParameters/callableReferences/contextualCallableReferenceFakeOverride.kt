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
    // unbound dispatch receiver typed as Base, dynamic dispatch must reach the override
    val viaBase: (Base) -> String = Base::f
    if (viaBase(Derived()) != "derived-ctx") return@context "FAIL 1: ${viaBase(Derived())}"
    if (viaBase(Base()) != "base-ctx") return@context "FAIL 2: ${viaBase(Base())}"

    // bound receiver of the derived type
    val bound: () -> String = Derived()::f
    if (bound() != "derived-ctx") return@context "FAIL 3: ${bound()}"

    // property fake override referenced through the subtype
    val prop: (Derived) -> String = Derived::p
    if (prop(Derived()) != "basep-ctx") return@context "FAIL 4: ${prop(Derived())}"

    "OK"
}
