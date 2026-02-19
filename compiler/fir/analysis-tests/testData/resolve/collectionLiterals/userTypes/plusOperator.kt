// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <K> of(vararg k: K) = MyList<K>()
    }

    operator fun plus(other: MyList<T>): MyList<T> = []
}

operator fun <U> MyList<U>.plusAssign(other: MyList<U>) { }

fun <V> concat(a: MyList<V>, b: MyList<V>): MyList<V> = a + b

fun testVal() {
    val x: MyList<Int> = [1, 2, 3]
    x += [1, 2, 3]
    x += []
    x += <!ARGUMENT_TYPE_MISMATCH!>["string"]<!>
    x += <!ARGUMENT_TYPE_MISMATCH!>[null]<!>

    x + []
    x + [1, 2, 3]
    x + <!ARGUMENT_TYPE_MISMATCH!>["string"]<!>
    x + <!ARGUMENT_TYPE_MISMATCH!>[null]<!>
}

fun testVar() {
    var x: MyList<Int> = [1, 2, 3]
    x <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> [1, 2, 3]
    x <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> []
    x <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> ["string"]
    x <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> [null]
}

fun testConcat() {
    val a: MyList<Int> = concat([1, 2, 3], [4, 5, 6])
    val b: MyList<Int> = concat([], [])
    val c: MyList<Int> = concat(<!ARGUMENT_TYPE_MISMATCH!>[""]<!>, [1, 2, 3])
    val d = concat([1, 2, 3], [4, 5, 6])
    val e = concat([1, 2, 3], ["string"])
    val f = concat([], [null])
    val g = concat([1, 2, 3], [null])
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, funWithExtensionReceiver,
functionDeclaration, integerLiteral, localProperty, nullableType, objectDeclaration, operator, propertyDeclaration,
stringLiteral, typeParameter, vararg */
