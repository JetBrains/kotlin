// !DIAGNOSTICS: -UNUSED_VARIABLE
import java.util.*

fun bar(): String? = null

fun foo() {
    var x = ArrayList<String?>()
    x.add(null)
    x.add(bar())
    x.add("")

    x[0] = null
    x[0] = bar()
    x[0] = ""

    val b1: MutableList<String?> = x
    val b2: MutableList<String> = <!TYPE_MISMATCH!>x<!>
    val b3: List<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = x
}
