// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals, +UnnamedLocalVariables
// WITH_STDLIB
// LATEST_LV_DIFFERENCE
//  ^ suspicious changes in behavior when +EagerLambdaAnalysis
//  ^ testing collection literals in both versions

class Z {
    companion object {
        operator fun of(vararg x: () -> Unit): Z = Z()
        inline operator fun of(x: () -> Unit): Z = Z()
    }
}

fun foo(z: Z, a: Any) { }
fun bar(z: Z, a: Any, b: Any) { }

fun cond(): Boolean = true

fun test() {
    if (cond()) {
        var x: Int?
        x = 42
        foo([{ x = null }, {}], <!SMARTCAST_IMPOSSIBLE!>x<!>.plus(42)) // non-inline
    } else {
        // for reference
        var x: Int?
        x = 42
        foo(Z.of({ x = null }, {}), <!SMARTCAST_IMPOSSIBLE!>x<!>.plus(42))
    }

    if (cond()) {
        var y: Int?
        y = 42
        foo([{ y = null }], <!SMARTCAST_IMPOSSIBLE!>y<!>.plus(42)) // inline
    } else {
        var y: Int?
        y = 42
        foo(Z.of({ y = null }), <!SMARTCAST_IMPOSSIBLE!>y<!>.plus(42))
    }

    if (cond()) {
        var z: Int?
        z = 42
        val _: Z = [{ z = null }, {}] // non-inline
        <!SMARTCAST_IMPOSSIBLE!>z<!>.plus(42)
    } else {
        var z: Int?
        z = 42
        val _: Z = Z.of({ z = null }, {})
        <!SMARTCAST_IMPOSSIBLE!>z<!>.plus(42)
    }

    if (cond()) {
        var t: Int?
        t = 42
        val _: Z = [{ t = null }] // inline
        t<!UNSAFE_CALL!>.<!>plus(42)
    } else {
        var t: Int?
        t = 42
        val _: Z = Z.of({ t = null })
        <!SMARTCAST_IMPOSSIBLE!>t<!>.plus(42)
    }
}

fun test2() {
    if (cond()) {
        var x: Int? = 42
        bar([{ x = null }, {}], x!!, <!SMARTCAST_IMPOSSIBLE!>x<!>) // non-inline
    } else {
        var x: Int? = 42
        bar(Z.of({ x = null }, {}), x!!, <!SMARTCAST_IMPOSSIBLE!>x<!>)
    }

    if (cond()) {
        var y: Int? = 42
        bar([{ y = null }], y!!, <!SMARTCAST_IMPOSSIBLE!>y<!>) // inline
    } else {
        var y: Int? = 42
        bar(Z.of({ y = null }), y!!, <!SMARTCAST_IMPOSSIBLE!>y<!>)
    }

    if (cond()) {
        var z: Int? = 42
        val resZ: Z = [{ z = null }, {}] // non-inline
        bar(resZ, z!!, <!SMARTCAST_IMPOSSIBLE!>z<!>)
    } else {
        var z: Int? = 42
        val resZ: Z = Z.of({ z = null }, {})
        bar(resZ, z!!, <!SMARTCAST_IMPOSSIBLE!>z<!>)
    }

    if (cond()) {
        var t: Int? = 42
        val resT: Z = [{ t = null }] // inline
        bar(resT, t!!, t)
    } else {
        var t: Int? = 42
        val resT: Z = Z.of({ t = null })
        bar(resT, t!!, <!SMARTCAST_IMPOSSIBLE!>t<!>)
    }
}

fun test3() {
    if (cond()) {
        var x: Int? = 42
        bar([{ x = 42 }, {}], x!!, <!SMARTCAST_IMPOSSIBLE!>x<!>) // non-inline
    } else {
        var x: Int? = 42
        bar(Z.of({ x = 42 }, {}), x!!, <!SMARTCAST_IMPOSSIBLE!>x<!>)
    }

    if (cond()) {
        var y: Int? = 42
        bar([{ y = 42 }], y!!, <!SMARTCAST_IMPOSSIBLE!>y<!>) // inline
    } else {
        var y: Int? = 42
        bar(Z.of({ y = 42 }), y!!, <!SMARTCAST_IMPOSSIBLE!>y<!>)
    }

    if (cond()) {
        var z: Int? = 42
        val resZ: Z = [{ z = 42 }, {}] // non-inline
        bar(resZ, z!!, z)
    } else {
        var z: Int? = 42
        val resZ: Z = Z.of({ z = 42 }, {})
        bar(resZ, z!!, z)
    }

    if (cond()) {
        var t: Int? = 42
        val resT: Z = [{ t = 42 }] // inline
        bar(resT, t!!, t)
    } else {
        var t: Int? = 42
        val resT: Z = Z.of({ t = 42 })
        bar(resT, t!!, t)
    }
}

fun test4() {
    if (cond()) {
        var x: Int? = 42
        val resX: Z = [{ x = null }, { }] // non-inline
        x = 42
        foo(resX, <!SMARTCAST_IMPOSSIBLE!>x<!>)
    } else {
        var x: Int? = 42
        val resX: Z = Z.of({ x = null }, { })
        x = 42
        foo(resX, <!SMARTCAST_IMPOSSIBLE!>x<!>)
    }

    if (cond()) {
        var y: Int? = 42
        val resY: Z = [{ y = null }] // inline
        y = 42
        foo(resY, y)
    } else {
        var y: Int? = 42
        val resY: Z = Z.of({ y = null })
        y = 42
        foo(resY, <!SMARTCAST_IMPOSSIBLE!>y<!>)
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, inline, integerLiteral,
lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration, smartcast, vararg */
