// PROBLEM: none
package p

class C

fun C.get(s: String) = s

fun foo(c: C) {
    c.<caret>get("x")
}
