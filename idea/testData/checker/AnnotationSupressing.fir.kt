annotation class A(val i: Int)
annotation class Z(val i: Int)

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error> @Suppress("TYPE_MISMATCH")
fun some0() {}

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error> <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error> @Suppress("TYPE_MISMATCH")
fun some01() {}

@Suppress("TYPE_MISMATCH") <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error>
fun some1() {
}

@Suppress("TYPE_MISMATCH") <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error> <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error>
fun some11() {
}

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /A.A">@A("BAD")</error> @Suppress("TYPE_MISMATCH")
fun some2() {
}

@Suppress("TYPE_MISMATCH") <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /A.A">@A("BAD")</error>
fun some3() {
}

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /A.A">@A("BAD")</error> <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /A.A">@A("BAD")</error>
fun some4() {
}

<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /Z.Z">@Z("BAD")</error>
fun someN() {
}
