// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
<!FUNCTION_DECLARATION_WITH_NO_NAME, CONFLICTING_OVERLOADS!>fun ()<!> {

}

<!FUNCTION_DECLARATION_WITH_NO_NAME!>fun Outer.()<!> {

}

<!REDECLARATION!>val<!SYNTAX!><!> : Int = 1<!>

<!REDECLARATION!>class<!SYNTAX!><!> {

}<!>

object<!SYNTAX!><!> {

}

interface<!SYNTAX!><!> {

}

enum class<!SYNTAX!><!> {

}

annotation class<!SYNTAX!><!> {

}

class Outer {
    <!FUNCTION_DECLARATION_WITH_NO_NAME!>fun ()<!> {

    }

    <!REDECLARATION!>val<!SYNTAX!><!> : Int = 1<!>

    <!REDECLARATION!>class<!SYNTAX!><!> {

    }<!>

    <!REDECLARATION!>object<!><!SYNTAX!><!> {

    }

    <!REDECLARATION!>interface<!SYNTAX!><!> {

    }<!>

    <!REDECLARATION!>enum class<!SYNTAX!><!> {

    }<!>

    <!REDECLARATION!>annotation class<!SYNTAX!><!> {

    }<!>
}

fun outerFun() {
    <!UNUSED_EXPRESSION!>fun () {

    }<!>
    <!UNUSED_EXPRESSION!>fun () {

    }<!>
}

