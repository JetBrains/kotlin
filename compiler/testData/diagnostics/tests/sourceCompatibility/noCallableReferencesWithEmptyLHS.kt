// !LANGUAGE: -CallableReferencesToClassMembersWithEmptyLHS

class A {
    fun memberFunction() {}
    val memberProperty: Int get() = 42

    fun test() {
        (::<!UNSUPPORTED_FEATURE!>memberFunction<!>)()
        (::<!UNSUPPORTED_FEATURE!>extensionFunction<!>)()
        (::<!UNSUPPORTED_FEATURE!>memberProperty<!>)()
        (::<!UNSUPPORTED_FEATURE!>extensionProperty<!>)()
    }

    inner class B {
        fun memberFunction() { }
        val memberProperty: Int get() = 43

        fun test() {
            (::<!UNSUPPORTED_FEATURE!>memberFunction<!>)()
            (::<!UNSUPPORTED_FEATURE!>extensionFunction<!>)()
            (::<!UNSUPPORTED_FEATURE!>memberProperty<!>)()
            (::<!UNSUPPORTED_FEATURE!>extensionProperty<!>)()
        }
    }
}

fun A.extensionFunction() {}
val A.extensionProperty: Int get() = 44
