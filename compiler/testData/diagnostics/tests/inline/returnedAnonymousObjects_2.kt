// SKIP_TXT
// ISSUE: KT-33917

class Inv<T>(val value: T)

private inline fun foo10(crossinline f: () -> Int) = object {
    fun bar(): Int = f()
}

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo11<!>(crossinline f: () -> Int) = Inv(object {
    fun bar(): Int = f()
})

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo12<!>(crossinline f: () -> Int) = Inv(Inv(object {
    fun bar(): Int = f()
}))

interface I1
interface I2

private inline fun foo20(crossinline f: () -> Int) = object : I1 {
    fun bar(): Int = f()
}

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo21<!>(crossinline f: () -> Int) = Inv(object : I1 {
    fun bar(): Int = f()
})

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo22<!>(crossinline f: () -> Int) = Inv(Inv(object : I1 {
    fun bar(): Int = f()
}))

<!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo30<!>(crossinline f: () -> Int)<!> = object : I1, I2 {
    fun bar(): Int = f()
}

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo31<!>(crossinline f: () -> Int) = Inv(object : I1, I2 {
    fun bar(): Int = f()
})

private inline fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>foo32<!>(crossinline f: () -> Int) = Inv(Inv(object : I1, I2 {
    fun bar(): Int = f()
}))

private fun foo40(f: () -> Int) = object {
    fun bar(): Int = f()
}

private fun foo41(f: () -> Int) = Inv(object {
    fun bar(): Int = f()
})

private fun foo42(f: () -> Int) = Inv(Inv(object {
    fun bar(): Int = f()
}))

// ------------------------------------------------------------------------------------------------

fun test10(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>foo10 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>foo10 { 2 }<!>
    }
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
}

fun test11(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo11.`<no name provided>`")!>foo11 { 1 }.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo11.`<no name provided>`")!>foo11 { 2 }.value<!>
    }
    x.bar()
}

fun test12(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo12.`<no name provided>`")!>foo12 { 1 }.value.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo12.`<no name provided>`")!>foo12 { 2 }.value.value<!>
    }
    x.bar()
}

// ------------------------------------------------------------------------------------------------

fun test20(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("I1")!>foo20 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("I1")!>foo20 { 2 }<!>
    }
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
}

fun test21(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo21.`<no name provided>`")!>foo21 { 1 }.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo21.`<no name provided>`")!>foo21 { 2 }.value<!>
    }
    x.bar()
}

fun test22(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo22.`<no name provided>`")!>foo22 { 1 }.value.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo22.`<no name provided>`")!>foo22 { 2 }.value.value<!>
    }
    x.bar()
}

// ------------------------------------------------------------------------------------------------

fun test30(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo30.`<no name provided>`")!>foo30 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo30.`<no name provided>`")!>foo30 { 2 }<!>
    }
    x.bar()
}

fun test31(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo31.`<no name provided>`")!>foo31 { 1 }.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo31.`<no name provided>`")!>foo31 { 2 }.value<!>
    }
    x.bar()
}

fun test32(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo32.`<no name provided>`")!>foo32 { 1 }.value.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo32.`<no name provided>`")!>foo32 { 2 }.value.value<!>
    }
    x.bar()
}

// ------------------------------------------------------------------------------------------------

fun test40(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo40.`<no name provided>`")!>foo40 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo40.`<no name provided>`")!>foo40 { 2 }<!>
    }
    x.bar()
}

fun test41(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo41.`<no name provided>`")!>foo41 { 1 }.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo41.`<no name provided>`")!>foo41 { 2 }.value<!>
    }
    x.bar()
}

fun test42(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("foo42.`<no name provided>`")!>foo42 { 1 }.value.value<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("foo42.`<no name provided>`")!>foo42 { 2 }.value.value<!>
    }
    x.bar()
}
