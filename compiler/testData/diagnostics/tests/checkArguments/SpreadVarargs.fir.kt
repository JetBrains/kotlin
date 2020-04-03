// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions

fun <T> array1(vararg a : T) = a

fun main() {
    val a = array1("a", "b")
    val b = array1(1, 1)
    join(1)
    join(1, "2")
    join(1, "2", "3")
    <!INAPPLICABLE_CANDIDATE!>join<!>(*1, "2")
    <!INAPPLICABLE_CANDIDATE!>join<!>(1, *"2")
    <!INAPPLICABLE_CANDIDATE!>join<!>(x = 1, a = "2")
    <!INAPPLICABLE_CANDIDATE!>join<!>(x = *1, a = *"2")
    join(x = 1, a = a)
    <!INAPPLICABLE_CANDIDATE!>join<!>(x = 1, a = b)
    join(1, *a)
    <!INAPPLICABLE_CANDIDATE!>join<!>(1, *b)
    join(1, *a, "3")
    <!INAPPLICABLE_CANDIDATE!>join<!>(1, *b, "3")
    join(1, "4", *a, "3")
    <!INAPPLICABLE_CANDIDATE!>join<!>(1, "4", *b, "3")
    join(1, "4", *a)
    join(1, "4", *a, *a, "3")
    <!INAPPLICABLE_CANDIDATE!>join<!>(1, "4", *a, *b, "3")
    join(a = a, x = 1)
    <!INAPPLICABLE_CANDIDATE!>join<!>(a = b, x = 1)
    join(a = a, x = 1)

    joinG<String>(1, "2")
    <!INAPPLICABLE_CANDIDATE!>joinG<!><String>(*1, "2")
    <!INAPPLICABLE_CANDIDATE!>joinG<!><String>(1, *"2")
    joinG<String>(x = 1, a = a)
    <!INAPPLICABLE_CANDIDATE!>joinG<!><String>(x = 1, a = "2")
    <!INAPPLICABLE_CANDIDATE!>joinG<!><String>(x = *1, a = *"2")
    joinG<String>(1, *a)
    joinG<String>(1, *a, "3")
    joinG<String>(1, "4", *a, "3")
    joinG<String>(1, "4", *a)
    joinG<String>(1, "4", *a, *a, "3")
    joinG<String>(a = a, x = 1)

    joinG(1, "2")
    <!INAPPLICABLE_CANDIDATE!>joinG<!>(*1, "2")
    <!INAPPLICABLE_CANDIDATE!>joinG<!>(1, *"2")
    joinG(x = 1, a = a)
    <!INAPPLICABLE_CANDIDATE!>joinG<!>(x = 1, a = "2")
    <!INAPPLICABLE_CANDIDATE!>joinG<!>(x = *1, a = *"2")
    joinG(1, *a)
    joinG(1, *a, "3")
    joinG(1, "4", *a, "3")
    joinG(1, "4", *a)
    joinG(1, "4", *a, *a, "3")
    joinG(a = a, x = 1)

    val x1 = joinT(1, "2")
    checkSubtype<String?>(x1)
    val x2 = <!INAPPLICABLE_CANDIDATE!>joinT<!>(*1, "2")
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><String?>(x2)
    val x6 = joinT(1, *a)
    checkSubtype<String?>(x6)
    val x7 = joinT(1, *a, "3")
    checkSubtype<String?>(x7)
    val x8 = joinT(1, "4", *a, "3")
    checkSubtype<String?>(x8)
    val x9 = joinT(1, "4", *a)
    checkSubtype<String?>(x9)
    val x10 = joinT(1, "4", *a, *a, "3")
    checkSubtype<String?>(x10)
    val x11 = joinT(a = a, x = 1)
    checkSubtype<String?>(x11)
    val x12 = joinT(x = 1, a = a)
    checkSubtype<String?>(x12)
}

fun join(x : Int, vararg a : String) : String {
    val b = StringBuilder(x.toString())
    for (s in a) {
        b.append(s)
    }
    return b.toString()
}

fun <T> joinG(x : Int, vararg a : T) : String {
    val b = StringBuilder(x.toString())
    for (s in a) {
        b.append(s)
    }
    return b.toString()
}

fun <T: Any> joinT(x : Int, vararg a : T) : T? {
    return null
}