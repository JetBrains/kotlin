// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}

fun <T : String?> foo(x: T) {
    x.<!INAPPLICABLE_CANDIDATE!>length<!>
    x?.length

    if (1 == 1) {
        x!!.length
    }


    x.bar1()
    x.bar2()

    x?.bar1()
    x?.bar2()

    x.<!INAPPLICABLE_CANDIDATE!>bar3<!>()

    x?.let { it.length }
}
