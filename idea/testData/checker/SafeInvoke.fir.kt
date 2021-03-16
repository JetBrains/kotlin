// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class Rule(val apply: () -> Unit)

fun foo() {
    val rule: Rule? = Rule { }
    rule?.apply()
    val apply = rule?.apply
    <error descr="[UNSAFE_IMPLICIT_INVOKE_CALL] Reference has a nullable type 'kotlin/Function0<kotlin/Unit>?', use explicit \"?.invoke\" to make a function-like call instead.">apply</error>()
}
