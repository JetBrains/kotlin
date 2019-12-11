// !DIAGNOSTICS: -REDECLARATION -DUPLICATE_CLASS_NAMES

class C {
    fun () {

    }

    val<!SYNTAX!><!> : Int = 1

    class<!SYNTAX!><!> {}

    enum class<!SYNTAX!><!> {}
}

class C1<<!SYNTAX!>in<!>> {}

class C2(val<!SYNTAX!><!>) {}