// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, subtyping, subtyping-for-intersection-types -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: type-system, subtyping, subtyping-for-intersection-types -> paragraph 2 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: intersection type inferred for parametrized functions
 * HELPERS: checkType, functions
 */


// TESTCASE NUMBER: 1
fun case1() {
    val x = case1(object : A1, B1 {})
    checkSubtype<A1>(x)
    checkSubtype<B1>(x)
}


interface A1 {
    fun fooA() = "A"
}

interface B1 {
    fun fooB() = "B"
}

fun <T> case1(case: T): T where T : A1, T : B1 = case

// TESTCASE NUMBER: 2
fun case2() {
    fun <T> case2(case: T): T where T : A2, T : B2 = case

    val x = case2(C2())
    checkSubtype<A2>(x)
    checkSubtype<B2>(x)
}

class C2: A2, B2

interface A2 {
    fun fooA() = "A"
}

interface B2 {
    fun fooB() = "B"
}

// TESTCASE NUMBER: 3
fun case3() {
    val x = case3(object : A3, B3 {})
    checkSubtype<A3>(x)
    checkSubtype<B3>(x)
}


interface A3 {
    fun fooA() = "A"
}

interface B3 {
    fun fooB() = "B"
}

fun <T> case3(case: T): T where T : A3, T : B3 = case

// TESTCASE NUMBER: 4
fun case4() {
    fun <T> case4(case: T): T where T : A4, T : B4 = case

    val x = case4(C4())
    checkSubtype<A4>(x)
    checkSubtype<B4>(x)
}

class C4: A4, B4

interface A4 {
    fun fooA() = "A"
}

interface B4 {
    fun fooB() = "B"
}



// TESTCASE NUMBER: 5
fun case5() {
    val x =  "" case5 object : A5, B5 {}
    checkSubtype<A5>(x)
    checkSubtype<B5>(x)
}


interface A5 {
    fun fooA() = "A"
}

interface B5 {
    fun fooB() = "B"
}

infix fun <T> CharSequence.case5(case: T): T where T : A5, T : B5 = case

// TESTCASE NUMBER: 6
fun case6() {
    infix fun <T> CharSequence.case6(case: T): T where T : A6, T : B6 = case

    val x = "" case6 C6()
    checkSubtype<A6>(x)
    checkSubtype<B6>(x)
}

class C6: A6, B6

interface A6 {
    fun fooA() = "A"
}

interface B6 {
    fun fooB() = "B"
}

// TESTCASE NUMBER: 7
fun case7() {
    val x = case7(object : A7, B7 {})
    checkSubtype<A7>(x)
    checkSubtype<B7>(x)
}


interface A7 {
    fun fooA() = "A"
}

interface B7 {
    fun fooB() = "B"
}

fun <T> case7(case: T): T where T : A7, T : B7 = case

// TESTCASE NUMBER: 8
fun case8() {
    infix fun <T> CharSequence.case8(case: T): T where T : A8, T : B8 = case

    val x = "" case8 C8()
    checkSubtype<A8>(x)
    checkSubtype<B8>(x)
}

class C8: A8, B8

interface A8 {
    fun fooA() = "A"
}

interface B8 {
    fun fooB() = "B"
}