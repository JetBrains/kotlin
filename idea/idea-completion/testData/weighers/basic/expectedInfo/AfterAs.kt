class Xx1
class Xx2
class Xx3

fun foo(xx: Xx2){}

fun bar(o: Any) {
    foo(o as Xx<caret>)
}

// ORDER: Xx2
// ORDER: Xx1
// ORDER: Xx3
