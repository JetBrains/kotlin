class Flower() {

    var minusOne: Int = 1
        get() = field + 1
        set(n: Int) { field = n - 1 }

    var oldSyntax: Int = 1
        get() = <!BACKING_FIELD_SYNTAX_DEPRECATED!>$oldSyntax<!> - 1
        set(arg) { <!BACKING_FIELD_SYNTAX_DEPRECATED!>$oldSyntax<!> = arg + 1 }

}
