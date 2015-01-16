abstract class C(i: Int){}

fun foo(p: (Int) -> C){}

fun bar(){
    foo(<caret>)
}

// ABSENT: ::C
