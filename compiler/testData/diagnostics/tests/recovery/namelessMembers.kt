// !DIAGNOSTICS: -REDECLARATION

class C {
    fun<!SYNTAX!><!> () {

    }

    val<!SYNTAX!><!> : Int = 1

    class<!SYNTAX!><!> {}

    enum class<!SYNTAX!><!> {}
}

class C1<in<!SYNTAX!>><!><!SYNTAX!><!> {}

class C2(val<!SYNTAX!><!>) {}