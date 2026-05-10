// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

class Z {
    companion object {
        inline operator fun of(first: () -> Z, vararg other: () -> Z): Z = Z()
    }
}

fun takeZ(z: Z) { }

fun testOk() {
    var a: Any?
    takeZ([{ a = null; Z() }, { Z() }])
    a = 42
    a.plus(42)

    var b: Any?
    takeZ([ { [ { [ { b = null; [ { Z() } ] } ] } ] } ])
    b = 42
    b.plus(42)

    var c: Any?
    val z: Set<Z> = [[ { c = null; [ { Z() } ] } ]]
    c = 42
    c.plus(42)
}

fun testFail() {
    var a: Any?
    takeZ([{ Z() }, { a = null; Z() }])
    a = 42
    <!SMARTCAST_IMPOSSIBLE!>a<!>.plus(42)

    var b: Any?
    takeZ([ { [ { Z() }, { [ { b = null; [ { Z() } ] } ] } ] } ])
    b = 42
    <!SMARTCAST_IMPOSSIBLE!>b<!>.plus(42)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, companionObject, functionDeclaration, functionalType, inline,
integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, propertyDeclaration, smartcast,
vararg */
