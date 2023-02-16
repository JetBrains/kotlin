// !API_VERSION: 1.3
// !OPT_IN: kotlin.RequiresOptIn
// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -NEWER_VERSION_IN_SINCE_KOTLIN -UNUSED_PARAMETER

@SinceKotlin("1.4")
fun newPublishedFun() {}


@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class Marker

@SinceKotlin("1.4")
@WasExperimental(Marker::class)
fun newFunExperimentalInThePast() {}

@SinceKotlin("1.4")
@WasExperimental(Marker::class)
val newValExperimentalInThePast = ""

@SinceKotlin("1.4")
@WasExperimental(Marker::class)
class NewClassExperimentalInThePast

@SinceKotlin("1.4")
@WasExperimental(Marker::class)
typealias TypeAliasToNewClass = <!OPT_IN_USAGE_ERROR!>NewClassExperimentalInThePast<!>


fun use1(
    c1: <!OPT_IN_USAGE_ERROR!>NewClassExperimentalInThePast<!>,
    t1: <!OPT_IN_USAGE_ERROR!>TypeAliasToNewClass<!>
) {
    <!UNRESOLVED_REFERENCE!>newPublishedFun<!>()
    <!OPT_IN_USAGE_ERROR!>newFunExperimentalInThePast<!>()
    <!OPT_IN_USAGE_ERROR!>newValExperimentalInThePast<!>
    <!OPT_IN_USAGE_ERROR!>NewClassExperimentalInThePast<!>()
}

@OptIn(Marker::class)
fun use2(
    c2: NewClassExperimentalInThePast,
    t2: TypeAliasToNewClass
) {
    <!UNRESOLVED_REFERENCE!>newPublishedFun<!>()
    newFunExperimentalInThePast()
    newValExperimentalInThePast
    NewClassExperimentalInThePast()
}

@Marker
fun use3(
    c3: NewClassExperimentalInThePast,
    t3: TypeAliasToNewClass
) {
    <!UNRESOLVED_REFERENCE!>newPublishedFun<!>()
    newFunExperimentalInThePast()
    newValExperimentalInThePast
    NewClassExperimentalInThePast()
}
