import java.util.*
fun foo() {
    val al = ArrayList<String>()
    al.size
    al.<!INAPPLICABLE_CANDIDATE!>contains<!>(1)
    al.contains("")

    al.remove("")
    al.removeAt(1)

    val hs = HashSet<String>()
    hs.size
    hs.<!INAPPLICABLE_CANDIDATE!>contains<!>(1)
    hs.contains("")

    hs.remove("")


    val hm = HashMap<String, Int>()
    hm.size
    hm.<!INAPPLICABLE_CANDIDATE!>containsKey<!>(1)
    hm.containsKey("")

    <!INAPPLICABLE_CANDIDATE!>hm[1]<!>
    hm[""]

    hm.remove("")
}
