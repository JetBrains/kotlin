// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-68606, KT-71024
// WITH_STDLIB
interface Foo<E>

data class SerializerAndValue<T>(
    val serializer: Foo<T>,
    val value: T
)

fun <T> encodeToString(serializer: Foo<T>, value: T) {}

val a: (SerializerAndValue<*>) -> Unit = { (serializer, value) ->
    encodeToString(serializer, <!ARGUMENT_TYPE_MISMATCH!>value<!>)
}

val b: (SerializerAndValue<*>) -> Unit = { serializerAndValue ->
    encodeToString(serializerAndValue.serializer, <!ARGUMENT_TYPE_MISMATCH!>serializerAndValue.value<!>)
}

val c: SerializerAndValue<*>.() -> Unit = {
    encodeToString(serializer, value)
}

val d: SerializerAndValue<*>.() -> Unit = {
    encodeToString(this.serializer, <!ARGUMENT_TYPE_MISMATCH!>this.value<!>)
}

fun test(a: Array<*>) {
    a.set(0, <!MEMBER_PROJECTED_OUT!>a.get(0)<!>)
}

fun Array<*>.reverse() {
    for (i in 0..(size / 2)) {
        val t = get(i)
        set(i, get(size - i - 1))
        set(size - i - 1, <!MEMBER_PROJECTED_OUT!>t<!>)
    }
}

fun assignedToLocalVariable(l: MutableCollection<*>) {
    val x = l.first()
    if (l is MutableList<*>) {
        l.get(0)
    }
    l.add(<!MEMBER_PROJECTED_OUT!>x<!>)
}

fun different(a1: Array<*>, a2: Array<*>) {
    a1.set(0, <!MEMBER_PROJECTED_OUT!>a2.get(0)<!>)
}

fun stableVar(a1: Array<*>) {
    var tmp = a1
    tmp.set(0, <!MEMBER_PROJECTED_OUT!>tmp.get(0)<!>)
}

fun smartCast(a1: Array<*>) {
    var tmp: Any? = null
    tmp = a1
    tmp.set(0, <!MEMBER_PROJECTED_OUT!>tmp.get(0)<!>)
}

fun stableCalledInPlaceInline(a1: Array<*>) {
    var tmp = a1
    run {
        tmp.set(0, <!MEMBER_PROJECTED_OUT!>tmp.get(0)<!>)
    }
}

val topLevel: Array<*> = arrayOf(1)

fun stableTopLevel(a1: Array<*>) {
    topLevel.set(0, <!MEMBER_PROJECTED_OUT!>topLevel.get(0)<!>)
}

fun unstable(a1: Array<*>, a2: Array<*>) {
    var tmp = a1
    tmp.set(0.also { tmp = a2 }, <!MEMBER_PROJECTED_OUT!>tmp.get(0)<!>)
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, data, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, outProjection, primaryConstructor, propertyDeclaration, starProjection,
thisExpression, typeParameter, typeWithExtension */
