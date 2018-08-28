/**
 * Useless one
 */
enum class SomeEnum

fun use() {
    Some<caret>Enum::class
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnEnumClassReference.kt</i></font><br>public final enum class <b>SomeEnum</b> : <a href="psi_element://kotlin.Enum">Enum</a>&lt;<a href="psi_element://SomeEnum">SomeEnum</a>&gt;</pre></div><div class='content'><p>Useless one</p></div><table class='sections'></table>
