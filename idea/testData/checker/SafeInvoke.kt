class Rule(val apply: () -> Unit)

fun foo() {
    val rule: Rule? = Rule { }
    rule?.<error descr="[UNSAFE_IMPLICIT_INVOKE_CALL] Reference has a nullable type '(() -> Unit)?', use explicit '?.invoke()' to make a function-like call instead">apply</error>()
    val apply = rule?.apply
    <error descr="[UNSAFE_IMPLICIT_INVOKE_CALL] Reference has a nullable type '(() -> Unit)?', use explicit '?.invoke()' to make a function-like call instead">apply</error>()
}
