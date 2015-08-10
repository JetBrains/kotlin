class Xyz

fun foo(xyz: Xyz){}

fun bar(o: Any) {
    foo(o as Xy<caret>)
}

// EXIST: Xyz
// NOTHING_ELSE
