package magic

object Samples {
    fun sampleMagic() {
        castTextSpell("[asd] [dse] [asz]")
    }
}

fun sampleScroll() {
    val reader = Scroll("[asd] [dse] [asz]").reader()
    castTextSpell(reader.readAll())
}

/**
 * @sample Samples.sampleMagic
 * @sample sampleScroll
 */
fun <caret>castTextSpell(spell: String) {
    throw SecurityException("Magic prohibited outside Hogwarts")
}

//INFO: <pre><b>public</b> <b>fun</b> castTextSpell(spell: String): Unit <i>defined in</i> magic</pre><br/>
//INFO: <dl><dt><b>Samples:</b></dt><dd><a href="psi_element://Samples.sampleMagic"><code>Samples.sampleMagic</code></a><pre><code>
//INFO: castTextSpell("[asd] [dse] [asz]")
//INFO: </code></pre></dd><dd><a href="psi_element://sampleScroll"><code>sampleScroll</code></a><pre><code>
//INFO: val reader = Scroll("[asd] [dse] [asz]").reader()
//INFO: castTextSpell(reader.readAll())
//INFO: </code></pre></dd></dl>
