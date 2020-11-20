// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION,-UNUSED_VARIABLE

fun <T : CharSequence?> T.bar1() {}
fun CharSequence?.bar2() {}

fun <T : CharSequence> T.bar3() {}
fun CharSequence.bar4() {}

fun <T : String?> T.foo() {
    if (this != null) {
        if (this != null) {}

        length
        this?.length

        bar1()
        bar2()
        bar3()
        bar4()


        this?.bar1()
    }

    <!INAPPLICABLE_CANDIDATE!>length<!>

    if (this is String) {
        length
        this?.length

        bar1()
        bar2()
        bar3()
    }
}
