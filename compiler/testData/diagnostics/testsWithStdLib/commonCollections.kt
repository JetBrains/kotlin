// !WITH_NEW_INFERENCE

import java.util.*
fun foo() {
    val al = ArrayList<String>()
    al.size
    al.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains<!>(<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    al.contains("")

    al.remove("")
    al.removeAt(1)

    val hs = HashSet<String>()
    hs.size
    hs.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains<!>(<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    hs.contains("")

    hs.remove("")


    val hm = HashMap<String, Int>()
    hm.size
    hm.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>containsKey<!>(<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    hm.containsKey("")

    <!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>hm[<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>]<!>
    hm[""]

    hm.remove("")
}