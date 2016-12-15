/**
 * Useless one
 */
enum class SomeEnum

fun use() {
    Some<caret>Enum::class
}

//INFO: <pre><b>public</b> <b>final</b> <b>enum class</b> SomeEnum : <a href="psi_element://kotlin.Enum">Enum</a>&lt;<a href="psi_element://SomeEnum">SomeEnum</a>&gt; <i>defined in</i> root package</pre><p>Useless one</p>
