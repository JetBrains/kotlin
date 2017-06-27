interface OurFace
open class OurClass

fun context() {
    val v = object : OurClass(), OurFace {}
    v<caret>
}

//INFO: <pre><b>val</b> v: &lt;anonymous object : <a href="psi_element://OurClass">OurClass</a>, <a href="psi_element://OurFace">OurFace</a>&gt;</pre>
