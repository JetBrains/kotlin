typealias R = <!RECURSIVE_TYPEALIAS_EXPANSION!>R<!>

typealias L = <!RECURSIVE_TYPEALIAS_EXPANSION!>List<L><!>

typealias A = <!RECURSIVE_TYPEALIAS_EXPANSION!>B<!>
typealias B = <!RECURSIVE_TYPEALIAS_EXPANSION!>A<!>

typealias F1 = <!RECURSIVE_TYPEALIAS_EXPANSION!>(Int) -> F2<!>
typealias F2 = <!RECURSIVE_TYPEALIAS_EXPANSION!>(F1) -> Int<!>
typealias F3 = (F1) -> Int

val x: F3 = TODO()
