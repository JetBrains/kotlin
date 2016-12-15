enum class E {

}

fun use() {
    E.values<caret>()
}

//INFO: <b>public</b> <b>final</b> <b>fun</b> values(): Array&lt;<a href="psi_element://E">E</a>&gt; <i>defined in</i> E<p>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants.</p>
