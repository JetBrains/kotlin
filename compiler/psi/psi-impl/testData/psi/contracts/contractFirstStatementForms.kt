// COMPILATION_ERRORS
import kotlin.contracts.*
import kotlin.contracts.contract as aliasedContract

fun plain(x: Boolean) { contract { returns() implies x } }

fun qualified(x: Boolean) { kotlin.contracts.contract { returns() implies x } }

fun withLeadingCommentsAndSemicolon(x: Boolean) {
    // comment
    /* block comment */
    ;
    contract { returns() implies x }
}

fun parenthesized(x: Boolean) { (contract { returns() implies x }) }

fun labeled(x: Boolean) { l@ contract { returns() implies x } }

fun annotated(x: Boolean) { @Suppress("") contract { returns() implies x } }

fun backticked(x: Boolean) { `contract` { returns() implies x } }

fun notFirst(x: Boolean) { println(); contract { returns() implies x } }

fun aliased(x: Boolean) { aliasedContract { returns() implies x } }

fun spacedQualified(x: Boolean) { kotlin . contracts . contract { returns() implies x } }

fun otherCall() { println() }

fun labeledOtherCall() { l@ println() }

fun otherQualifiedCall() { kotlin.io.println() }

fun emptyBody() {}

fun expressionBody() = contract { }

val property: Int
    get() {
        contract { }
        return 0
    }

val propertyWithoutContract: Int
    get() {
        return 0
    }

class C {
    constructor() {
        contract { }
    }

    constructor(x: Int) {
        println(x)
    }
}
