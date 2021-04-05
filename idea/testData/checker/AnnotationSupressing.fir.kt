annotation class A(val i: Int)
annotation class Z(val i: Int)

@Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>) @Suppress("TYPE_MISMATCH")
fun some0() {}

@Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>) @Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>) @Suppress("TYPE_MISMATCH")
fun some01() {}

@Suppress("TYPE_MISMATCH") @Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>)
fun some1() {
}

@Suppress("TYPE_MISMATCH") @Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>) @Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>)
fun some11() {
}

@A(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>) @Suppress("TYPE_MISMATCH")
fun some2() {
}

@Suppress("TYPE_MISMATCH") @A(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>)
fun some3() {
}

@A(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>) @A(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>)
fun some4() {
}

@Z(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/String but kotlin/Int was expected">"BAD"</error>)
fun someN() {
}
