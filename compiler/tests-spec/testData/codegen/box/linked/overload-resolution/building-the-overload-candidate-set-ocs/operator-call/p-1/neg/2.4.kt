// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 1 -> sentence 2
 * SECONDARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 3
 * NUMBER: 4
 * DESCRIPTION: nullable receiver and infix functions
 * EXCEPTION: compiletime
 */

fun box(): String {
    val a: A? = A(B())
    val c: C = C()

    a?.b += c
    if (f3 && !f1 && !f2 && !f4) {
        f3 = false
        a?.b plusAssign c
        if (f3 && !f1 && !f2 && !f4)
            return "OK"
    }
    return "NOK"
}

class A(val b: B)

var f1 = false
var f2 = false
var f3 = false
var f4 = false

class B {
   infix operator fun plusAssign(c: C) {
        f1 = true
        print("1")
    }

    infix operator fun plus(c: C): C {
        f2 = true
        print("2")
        return c
    }
}

infix operator fun B.plusAssign(c: C) {
    f3 = true
    print("3")
}

infix operator fun B.plusAssign(c: Any) {
    f4 = true
    print("4")
}


class C