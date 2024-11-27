// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_EXTRA_CHECKERS
// DIAGNOSTICS: +UNUSED_EXPRESSION, +UNUSED_LAMBDA_EXPRESSION, -DEBUG_INFO_SMARTCAST, -ASSIGNED_VALUE_IS_NEVER_READ

fun run(block: () -> Unit) {}
fun Any.extensionFun() {}

const val constant = ""

class Dispatch {
    val property: Any = ""

    fun Any.extension(parameter: Any) {
        val variable: Any = ""

        <!UNUSED_EXPRESSION!>""<!>
        <!UNUSED_EXPRESSION!>""::class<!>
        <!UNUSED_EXPRESSION!>""::toString<!>
        <!UNUSED_EXPRESSION!>""::extensionFun<!>
        <!UNUSED_EXPRESSION!>""::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        constant
        constant::class
        constant::toString
        constant::extensionFun
        constant::<!UNRESOLVED_REFERENCE!>unknownFun<!>

        "constant=${constant}"
        "constant=${constant}"::class
        "constant=${constant}"::toString
        "constant=${constant}"::extensionFun
        "constant=${constant}"::<!UNRESOLVED_REFERENCE!>unknownFun<!>

        <!UNUSED_EXPRESSION!>parameter<!>
        <!UNUSED_EXPRESSION!>parameter::class<!>
        <!UNUSED_EXPRESSION!>parameter::toString<!>
        <!UNUSED_EXPRESSION!>parameter::extensionFun<!>
        <!UNUSED_EXPRESSION!>parameter::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        if (parameter is String) {
            <!UNUSED_EXPRESSION!>parameter<!>
            <!UNUSED_EXPRESSION!>parameter::class<!>
            <!UNUSED_EXPRESSION!>parameter::length<!>
            <!UNUSED_EXPRESSION!>parameter::extensionFun<!>
            <!UNUSED_EXPRESSION!>parameter::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>
        }

        <!UNUSED_EXPRESSION!>"parameter=${parameter}"<!>
        <!UNUSED_EXPRESSION!>"parameter=${parameter}"::class<!>
        <!UNUSED_EXPRESSION!>"parameter=${parameter}"::toString<!>
        <!UNUSED_EXPRESSION!>"parameter=${parameter}"::extensionFun<!>
        <!UNUSED_EXPRESSION!>"parameter=${parameter}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        if (parameter is String) {
            <!UNUSED_EXPRESSION!>"parameter=${parameter}"<!>
            <!UNUSED_EXPRESSION!>"parameter=${parameter}"::class<!>
            <!UNUSED_EXPRESSION!>"parameter=${parameter}"::length<!>
            <!UNUSED_EXPRESSION!>"parameter=${parameter}"::extensionFun<!>
            <!UNUSED_EXPRESSION!>"parameter=${parameter}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>
        }

        <!UNUSED_EXPRESSION!>this@Dispatch<!>
        <!UNUSED_EXPRESSION!>this@Dispatch::class<!>
        <!UNUSED_EXPRESSION!>this@Dispatch::toString<!>
        <!UNUSED_EXPRESSION!>this@Dispatch::extensionFun<!>
        <!UNUSED_EXPRESSION!>this@Dispatch::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        <!UNUSED_EXPRESSION!>"this@Dispatch=${this@Dispatch}"<!>
        <!UNUSED_EXPRESSION!>"this@Dispatch=${this@Dispatch}"::class<!>
        <!UNUSED_EXPRESSION!>"this@Dispatch=${this@Dispatch}"::toString<!>
        <!UNUSED_EXPRESSION!>"this@Dispatch=${this@Dispatch}"::extensionFun<!>
        <!UNUSED_EXPRESSION!>"this@Dispatch=${this@Dispatch}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        <!UNUSED_EXPRESSION!>this@extension<!>
        <!UNUSED_EXPRESSION!>this@extension::class<!>
        <!UNUSED_EXPRESSION!>this@extension::toString<!>
        <!UNUSED_EXPRESSION!>this@extension::extensionFun<!>
        <!UNUSED_EXPRESSION!>this@extension::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        if (this@extension is String) {
            <!UNUSED_EXPRESSION!>this@extension<!>
            <!UNUSED_EXPRESSION!>this@extension::class<!>
            <!UNUSED_EXPRESSION!>this@extension::length<!>
            <!UNUSED_EXPRESSION!>this@extension::extensionFun<!>
            <!UNUSED_EXPRESSION!>this@extension::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>
        }

        <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"<!>
        <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::class<!>
        <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::toString<!>
        <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::extensionFun<!>
        <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        if (this@extension is String) {
            <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"<!>
            <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::class<!>
            <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::length<!>
            <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::extensionFun<!>
            <!UNUSED_EXPRESSION!>"this@extension=${this@extension}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>
        }

        <!UNUSED_EXPRESSION!>variable<!>
        <!UNUSED_EXPRESSION!>variable::class<!>
        <!UNUSED_EXPRESSION!>variable::toString<!>
        <!UNUSED_EXPRESSION!>variable::extensionFun<!>
        <!UNUSED_EXPRESSION!>variable::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        if (variable is String) {
            <!UNUSED_EXPRESSION!>variable<!>
            <!UNUSED_EXPRESSION!>variable::class<!>
            <!UNUSED_EXPRESSION!>variable::length<!>
            <!UNUSED_EXPRESSION!>variable::extensionFun<!>
            <!UNUSED_EXPRESSION!>variable::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>
        }

        <!UNUSED_EXPRESSION!>"variable=${variable}"<!>
        <!UNUSED_EXPRESSION!>"variable=${variable}"::class<!>
        <!UNUSED_EXPRESSION!>"variable=${variable}"::toString<!>
        <!UNUSED_EXPRESSION!>"variable=${variable}"::extensionFun<!>
        <!UNUSED_EXPRESSION!>"variable=${variable}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        if (variable is String) {
            <!UNUSED_EXPRESSION!>"variable=${variable}"<!>
            <!UNUSED_EXPRESSION!>"variable=${variable}"::class<!>
            <!UNUSED_EXPRESSION!>"variable=${variable}"::length<!>
            <!UNUSED_EXPRESSION!>"variable=${variable}"::extensionFun<!>
            <!UNUSED_EXPRESSION!>"variable=${variable}"::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>
        }

        property
        property::class
        property::toString
        property::extensionFun
        property::<!UNRESOLVED_REFERENCE!>unknownFun<!>

        if (property is String) {
            property
            property::class
            property::length
            property::extensionFun
            property::<!UNRESOLVED_REFERENCE!>unknownFun<!>
        }

        "property=${property}"
        "property=${property}"::class
        "property=${property}"::toString
        "property=${property}"::extensionFun
        "property=${property}"::<!UNRESOLVED_REFERENCE!>unknownFun<!>

        if (property is String) {
            "property=${property}"
            "property=${property}"::class
            "property=${property}"::length
            "property=${property}"::extensionFun
            "property=${property}"::<!UNRESOLVED_REFERENCE!>unknownFun<!>
        }

        <!UNUSED_EXPRESSION!>Any::class<!>
        <!UNUSED_EXPRESSION!>Any::toString<!>
        <!UNUSED_EXPRESSION!>Any::extensionFun<!>
        <!UNUSED_EXPRESSION!>Any::<!UNRESOLVED_REFERENCE!>unknownFun<!><!>

        <!UNUSED_LAMBDA_EXPRESSION!>{}<!>
        {}.invoke()
        run({})
        run {}

        var i = 0
        ++i
        --i
        i++
        i--
        i += 1
        i -= 1
        i *= 1
        i /= 1

        val ints = IntArray(1)
        ++ints[0]
        --ints[0]
        ints[0]++
        ints[0]--
        ints[0] += 1
        ints[0] -= 1
        ints[0] *= 1
        ints[0] /= 1
    }
}
