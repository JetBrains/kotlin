class C<T>

fun <T> foo(p1: C<T>, p2: T){}

fun bar(s: String, o: Any) {
    foo(C<String>(), <caret>)
}

//EXIST: s
//ABSENT: o
