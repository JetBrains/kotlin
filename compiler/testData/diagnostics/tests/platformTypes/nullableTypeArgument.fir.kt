import java.util.ArrayList

fun foo() {
    val list = ArrayList<String?>()

    for (s in list) {
        s.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}