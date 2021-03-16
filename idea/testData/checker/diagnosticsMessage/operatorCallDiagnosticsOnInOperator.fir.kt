fun call() {
    val list : String? = "sdfknsdkfm"
    "x" <error descr="[UNSAFE_OPERATOR_CALL] Operator call corresponds to a dot-qualified call 'R|<local>/list|.contains(String(x))' which is not allowed on a nullable receiver 'R|<local>/list|'. ">in</error> list

    "x" <error descr="[UNSAFE_OPERATOR_CALL] Operator call corresponds to a dot-qualified call 'R|<local>/list|.contains(String(x))' which is not allowed on a nullable receiver 'R|<local>/list|'. ">!in</error> list
}

operator fun CharSequence.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean = true
