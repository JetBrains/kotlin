fun foo(x: Int) {}
fun baz(s: String) {}

const val TRUE = true

fun bar(s: String?) {
    foo(if (TRUE) 1 else 2)

    foo(if (false && TRUE) {
        baz("a")
        1
    } else 2)

    if (true) {
        //asd
        baz("a")
        baz("b")
    }

    if (false) {
        baz("a")
    }

    foo(if (TRUE) {
        baz("a")
        1
    } else 2)

    if (s ==null) {
        1
    } else if (true) {
        2
    }

    if (s ==null) {
        1
    } else if (true) 2
}