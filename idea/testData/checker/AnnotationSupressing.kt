annotation class A(val i: Int)
annotation class Z(val i: Int)

@Z("BAD") @Suppress("TYPE_MISMATCH")
fun some0() {}

@Z("BAD") <error descr="[REPEATED_ANNOTATION] This annotation is not repeatable">@Z("BAD")</error> @Suppress("TYPE_MISMATCH")
fun some01() {}

@Suppress("TYPE_MISMATCH") @Z("BAD")
fun some1() {
}

@Suppress("TYPE_MISMATCH") @Z("BAD") <error descr="[REPEATED_ANNOTATION] This annotation is not repeatable">@Z("BAD")</error>
fun some11() {
}

@A("BAD") @Suppress("TYPE_MISMATCH")
fun some2() {
}

@Suppress("TYPE_MISMATCH") @A("BAD")
fun some3() {
}

@A(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but Int was expected">"BAD"</error>) <error descr="[REPEATED_ANNOTATION] This annotation is not repeatable">@A(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but Int was expected">"BAD"</error>)</error>
fun some4() {
}

@Z(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but Int was expected">"BAD"</error>)
fun someN() {
}
