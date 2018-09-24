/**
 * Enum of 1, 2
 */
enum class SomeEnum(val i: Int) {
    One(1), Two(2);
}

fun use() {
    Some<caret>Enum.One
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnEnumUsage.kt</i></font><br>public final enum class <b>SomeEnum</b> : <a href="psi_element://kotlin.Enum">Enum</a>&lt;<a href="psi_element://SomeEnum">SomeEnum</a>&gt;</pre></div><div class='content'><p>Enum of 1, 2</p></div><table class='sections'></table>
