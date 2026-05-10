// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class Z {
    companion object {
        inline operator fun of(first: () -> Z, vararg other: () -> Z): Z = Z()
    }
}

val returnZ: () -> Z = ::Z

class Y {
    companion object {
        operator fun of(first: () -> Y, vararg other: () -> Y): Y = Y()
    }
}

val returnY: () -> Y = ::Y

object A {
    operator fun plus(z: Z): A = this
    operator fun plusAssign(y: Y) { }
}

fun testOk() {
    var x: Any?
    var a = A
    a += [
        {
            x = null
            Z()
        },
        returnZ
    ]
    x = 42
    x.plus(42)
}

fun testFail() {
    var a = A
    var x: Any?
    a += [
        returnZ,
        {
            x = null
            Z()
        }
    ]
    x = 42
    <!SMARTCAST_IMPOSSIBLE!>x<!>.plus(42)

    var y: Any?
    a += [
        {
            y = null
            Y()
        },
        returnY
    ]
    y = 42
    <!SMARTCAST_IMPOSSIBLE!>y<!>.plus(42)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, callableReference, classDeclaration, companionObject,
functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral, localProperty, nullableType,
objectDeclaration, operator, propertyDeclaration, smartcast, thisExpression, vararg */
