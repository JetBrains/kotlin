// RUN_PIPELINE_TILL: BACKEND

interface A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

open class B

open class C {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean = true
}

open class D {
    override fun equals(@EqualityBound(D::class) other: Any?): Boolean = true
}

interface F

class G

class H {
    override fun equals(@EqualityBound(H::class) other: Any?): Boolean = true
}

fun test1(x: Any, y: Any): Boolean {
    if (x !is A || x !is B || y !is C) return false
    return <!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>x == y<!>
}

fun test2(x: Any, y: Any): Boolean {
    if (x !is A || x !is B || y !is G) return false
    return <!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>x == y<!>
}

fun test3(x: Any, y: Any): Boolean {
    if (x !is A || x !is B || y !is H) return false
    return <!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>x == y<!>
}

fun test4(x: Any, y: Any): Boolean {
    if (x !is A || x !is C || y !is B) return false
    return x == y
}

fun test5(x: Any, y: Any): Boolean {
    // Strictly speaking, EB(x) = A & C which should be incomatible with D
    // since C and D are unrelated _classes_. However, for open classes we only
    // consider types incompatible here if they are normal `ConeClassLikeType`s.
    if (x !is A || x !is C || y !is D) return false
    return <!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>x == y<!>
}

fun test6(x: Any, y: Any): Boolean {
    if (x !is F || x !is C || y !is D) return false
    return <!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>x == y<!>
}

fun test7(x: Any, y: Any): Boolean {
    if (x !is A || x !is C || y !is B || y !is F) return false
    return x == y
}

fun test8(x: Any, y: Any): Boolean {
    // x: A & B
    // EB(x) = A
    // y: C & F
    // EB(y) = C
    //  see above why it is not enough at the moment
    if (x !is A || x !is B || y !is C || y !is F) return false
    return <!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>x == y<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, disjunctionExpression, equalityExpression, functionDeclaration,
ifExpression, interfaceDeclaration, intersectionType, isExpression, nullableType, operator, override, smartcast */
