fun f1() {
    for (<!ELEMENT!>: Any in 0..10) {}

    val x1 = {<!ELEMENT!>: Boolean ->
        println("1")
    }

    val x2 = {<!ELEMENT!>: Boolean, <!ELEMENT!>: <!ELEMENT!> -> }

    var <!ELEMENT!>: Boolean;

    val x3 = fun(<!ELEMENT!>: Boolean) {

    }(<!ELEMENT!>)
}
