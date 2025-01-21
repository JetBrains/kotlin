// MODULE: main
// FILE: main.kt
package test

class A {
    val one: Int = 1
}

class A {
    val two: Int = 2
}

class T1

typealias A = T1

class B {
    val three: Int = 3
}

class A {
    val three: Int = 3
}

object C

class T2

typealias A = T2

// class_like: test/A
