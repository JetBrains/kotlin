// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String {
    val list = ArrayList<Pair<String,String>>()
    list.add(Pair("Sample", "http://cyber.law.harvard.edu/rss/examples/rss2sample.xml"))
    list.add(Pair("Scripting", "http://static.scripting.com/rss.xml"))

    val keys = list.map { it.first }.toTypedArray<String>()

    val keysToString = keys.contentToString()
    if (keysToString != "[Sample, Scripting]") return keysToString

    return "OK"
}
