// RUN_PIPELINE_TILL: BACKEND

open class Regular {
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true

    val p1: String get() = null!!
}

fun <T : Regular> useSite_1(x: T, y: Any?) {
    if (x == y) {
        y.p1.length
    }
}

interface A {
    val p2: String
}

interface B : A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

fun <T : B> useSite_2(x: T, y: Any?) {
    if (x == y) {
        y.p2.length
    }
}

fun <T : B?, S : T & Any> useSite_3(x: S, y: Any?) {
    if (x == y) {
        y.p2.length
    }
}

class OfClass<T : Regular>(val t: T, val p: Any?) {
    fun useSite_4() {
        if (t == p) {
            p.p1.length
        }
    }
}

abstract class Generic<X : Generic<X>>(val x: X) {
    abstract override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean
}

fun <T : Generic<T>> useSite_5(g: Generic<T>) {
    var a: Any? = null
    if (g.x == a) {
        a.x.x.x
    }
}

fun <T> useSite_6(g: Generic<T>) where T : Generic<T> {
    var a: Any? = null
    if (g.x == a || g == a) {
        a.x
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, classReference, dnnType, equalityExpression,
functionDeclaration, getter, ifExpression, interfaceDeclaration, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, typeConstraint, typeParameter */
