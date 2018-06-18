enum class E {

}

fun use() {
    E.values<caret>()
}

//INFO: <div class='definition'><pre><a href="psi_element://E"><code>E</code></a><br><b>public</b> <b>final</b> <b>fun</b> values(): Array&lt;<a href="psi_element://E">E</a>&gt;</pre></div><div class='content'><p>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants.</p></div><table class='sections'></table>
