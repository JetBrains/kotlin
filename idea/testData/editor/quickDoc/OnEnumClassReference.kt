/**
 * Useless one
 */
enum class SomeEnum

fun use() {
    Some<caret>Enum::class
}

//INFO: <div class='definition'><pre>(OnEnumClassReference.kt)<br><b>public</b> <b>final</b> <b>enum class</b> SomeEnum : <a href="psi_element://kotlin.Enum">Enum</a>&lt;<a href="psi_element://SomeEnum">SomeEnum</a>&gt;</pre></div><div class='content'><p>Useless one</p></div><table class='sections'></table>
