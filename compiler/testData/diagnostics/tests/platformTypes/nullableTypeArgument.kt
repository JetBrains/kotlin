import java.util.ArrayList

fun foo() {
    val list = ArrayList<String?>()

    for (s in list) {
        s<!UNSAFE_CALL!>.<!>length
    }
}