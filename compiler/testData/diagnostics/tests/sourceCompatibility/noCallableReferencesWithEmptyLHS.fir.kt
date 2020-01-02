// !LANGUAGE: -CallableReferencesToClassMembersWithEmptyLHS

class A {
    fun memberFunction() {}
    val memberProperty: Int get() = 42

    fun test() {
        (::memberFunction)()
        (::extensionFunction)()
        (::memberProperty)()
        (::extensionProperty)()
    }

    inner class B {
        fun memberFunction() { }
        val memberProperty: Int get() = 43

        fun test() {
            (::memberFunction)()
            (::extensionFunction)()
            (::memberProperty)()
            (::extensionProperty)()
        }
    }
}

fun A.extensionFunction() {}
val A.extensionProperty: Int get() = 44
