enum class E {
    A
}

fun use() {
    E.valueOf<caret>("A")
}


//INFO: <b>public</b> <b>final</b> <b>fun</b> valueOf(value: String): <a href="psi_element://E">E</a> <i>defined in</i> E<p>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)</p>
//INFO: <dl><dt><b>Throws:</b></dt><dd><code>IllegalArgumentException</code> - if this enum type has no constant with the specified name</dd></dl>
