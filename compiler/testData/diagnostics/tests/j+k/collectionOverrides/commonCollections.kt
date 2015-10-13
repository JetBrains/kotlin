import java.util.*
fun foo() {
    val al = ArrayList<String>()
    al.size
    al.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    al.contains("")

    al.remove("")
    al.removeAt(1)

    val hs = HashSet<String>()
    hs.size
    hs.contains(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    hs.contains("")

    hs.remove("")


    val hm = HashMap<String, Int>()
    hm.size
    hm.containsKey(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    hm.containsKey("")

    hm[<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>]
    hm[""]

    hm.remove("")
}
