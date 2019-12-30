// !WITH_NEW_INFERENCE
// !LANGUAGE: -NonStrictOnlyInputTypesChecks

import java.util.*
fun foo() {
    val al = ArrayList<String>()
    al.size
    al.contains(1)
    al.contains("")

    al.remove("")
    al.removeAt(1)

    val hs = HashSet<String>()
    hs.size
    hs.contains(1)
    hs.contains("")

    hs.remove("")


    val hm = HashMap<String, Int>()
    hm.size
    hm.containsKey(1)
    hm.containsKey("")

    hm[1]
    hm[""]

    hm.remove("")
}