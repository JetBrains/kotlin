import java.util.ArrayList

open class BaseStringList: ArrayList<String>() {
}

class StringList: BaseStringList() {
    public override fun get(index: Int): String {
        return "StringList.get()"
    }
}

fun box(): String {
    val myStringList = StringList()
    myStringList.add("first element")
    if (myStringList.get(0) != "StringList.get()") return "Fail #1"
    if ((myStringList: BaseStringList).get(0) != "StringList.get()") return "Fail #2"
    if ((myStringList: ArrayList<String>).get(0) != "StringList.get()") return "Fail #3"
    return "OK"
}
