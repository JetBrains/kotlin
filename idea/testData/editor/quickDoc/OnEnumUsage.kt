/**
 * Enum of 1, 2
 */
enum class SomeEnum(val i: Int) {
    One(1), Two(2);
}

fun use() {
    Some<caret>Enum.One
}

//INFO: <pre><b>public</b> <b>final</b> <b>enum class</b> SomeEnum : <a href="psi_element://kotlin.Enum">Enum</a>&lt;<a href="psi_element://SomeEnum">SomeEnum</a>&gt; <i>defined in</i> root package</pre><p>Enum of 1, 2</p>
