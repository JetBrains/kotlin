class A {
    var x1: String
        set(private <!ELEMENT!>) = <!ELEMENT!>

    var x2: String
        set(abstract final <!ELEMENT!>) {
            return field
        }
}

var x3 = 0L
    set(suspend <!ELEMENT!>) {
        return field
    }
