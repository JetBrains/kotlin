//KT-3007 Kotlin plugin 0.4.126 does not compile KAnnotator revision ba0a93eb
package a

enum class SomeEnum {
    FIRST,
    SECOND
}

// Doesn't work
fun Iterable<Int>.some() {
    this.fold(SomeEnum.FIRST, {res : SomeEnum, <!UNUSED_ANONYMOUS_PARAMETER!>value<!> ->
        if (res == SomeEnum.FIRST) SomeEnum.FIRST else SomeEnum.SECOND
    })
}

fun tempFun() : SomeEnum {
    return SomeEnum.FIRST
}

// Doesn't work
fun Iterable<Int>.someSimpleWithFun() {
    this.fold(SomeEnum.FIRST, {<!UNUSED_ANONYMOUS_PARAMETER!>res<!> : SomeEnum, <!UNUSED_ANONYMOUS_PARAMETER!>value<!> ->
        tempFun()
    })
}


// Works
fun Iterable<Int>.someSimple() {
    this.fold(SomeEnum.FIRST, {<!UNUSED_ANONYMOUS_PARAMETER!>res<!> : SomeEnum, <!UNUSED_ANONYMOUS_PARAMETER!>value<!> ->
        SomeEnum.FIRST
    })
}

// Works
fun Iterable<Int>.someInt() {
    this.fold(0, {res : Int, <!UNUSED_ANONYMOUS_PARAMETER!>value<!> ->
        if (res == 0) 1 else 0
    })
}

//from standard library
fun <T,R> Iterable<T>.fold(<!UNUSED_PARAMETER!>initial<!>: R, <!UNUSED_PARAMETER!>operation<!>: (R, T) -> R): R {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
