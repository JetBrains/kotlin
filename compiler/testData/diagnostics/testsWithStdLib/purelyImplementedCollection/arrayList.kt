// !DIAGNOSTICS: -UNUSED_VARIABLE
import java.util.*

fun bar(): String? = null

fun foo() {
    var x = ArrayList<String>()
    x.<!NONE_APPLICABLE!>add<!>(null)
    x.<!NONE_APPLICABLE!>add<!>(bar())
    x.add("")

    x[0] = <!NULL_FOR_NONNULL_TYPE!>null<!>
    x[0] = <!TYPE_MISMATCH!>bar()<!>
    x[0] = ""

    val b1: MutableList<String?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableList<String> = x
    val b3: List<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = <!TYPE_MISMATCH!>x<!>
}
