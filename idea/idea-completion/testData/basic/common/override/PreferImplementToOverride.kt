interface I {
    fun bbb()
}

open class Base {
    open fun aaa() {}
}

class A : Base(), I {
    overr<caret>
}

// WITH_ORDER
// EXIST: { itemText: "override" }
// EXIST: { itemText: "override fun bbb() {...}" }
// EXIST: { itemText: "override fun aaa() {...}" }
