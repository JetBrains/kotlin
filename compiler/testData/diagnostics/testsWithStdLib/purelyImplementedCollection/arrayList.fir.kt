// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
import java.util.*

fun bar(): String? = null

fun foo() {
    var x = ArrayList<String>()
    x.<!INAPPLICABLE_CANDIDATE!>add<!>(null)
    x.<!INAPPLICABLE_CANDIDATE!>add<!>(bar())
    x.add("")

    <!INAPPLICABLE_CANDIDATE!>x[0] = null<!>
    <!INAPPLICABLE_CANDIDATE!>x[0] = bar()<!>
    x[0] = ""

    val b1: MutableList<String?> = x
    val b2: MutableList<String> = x
    val b3: List<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = x
}