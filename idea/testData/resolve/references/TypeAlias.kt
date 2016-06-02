package test

class C

typealias CA = C

val x: <caret>CA = CA()

// REF: typealias CA = C
