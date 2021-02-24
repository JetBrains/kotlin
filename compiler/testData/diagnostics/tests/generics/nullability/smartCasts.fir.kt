// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}
fun CharSequence.bar4() {}

fun <T : CharSequence?> foo(x: T) {

    if (x != null) {
        if (x != null) {}

        x.length
        x?.length

        x.bar1()
        x.bar2()
        x.bar3()
        x.bar4()


        x?.bar1()
    }

    x.<!INAPPLICABLE_CANDIDATE!>length<!>

    if (x is String) {
        x.length
        x?.length

        x.bar1()
        x.bar2()
        x.bar3()
    }

    if (x is CharSequence) {
        x.length
        x?.length

        x.bar1()
        x.bar2()
        x.bar3()
    }
}
