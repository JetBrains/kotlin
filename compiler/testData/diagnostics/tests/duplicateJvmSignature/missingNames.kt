fun<!SYNTAX!><!> () {

}

val<!SYNTAX!><!> : Int = 1

class<!SYNTAX!><!> {

}

object <!SYNTAX!><!>{

}

trait<!SYNTAX!><!> {

}

enum class<!SYNTAX!><!> {

}

annotation class<!SYNTAX!><!> <!ANNOTATION_CLASS_WITH_BODY!>{

}<!>

class Outer {
    fun<!SYNTAX!><!> () {

    }

    <!REDECLARATION!>val<!SYNTAX!><!> : Int = 1<!>

    <!REDECLARATION!>class<!SYNTAX!><!> {

    }<!>

    <!REDECLARATION!>object<!> <!SYNTAX!><!>{

    }

    <!REDECLARATION!>trait<!SYNTAX!><!> {

    }<!>

    <!REDECLARATION!>enum class<!SYNTAX!><!> {

    }<!>

    <!REDECLARATION!>annotation class<!SYNTAX!><!> <!ANNOTATION_CLASS_WITH_BODY!>{

    }<!><!>
}