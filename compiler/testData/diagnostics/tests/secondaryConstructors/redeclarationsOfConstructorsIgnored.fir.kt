// RUN_PIPELINE_TILL: SOURCE
class <!CLASSIFIER_REDECLARATION!>A<!>
class <!CLASSIFIER_REDECLARATION!>A<!> {
    constructor()
}

class B
class Outer {
    class B {
        constructor()
    }
}
