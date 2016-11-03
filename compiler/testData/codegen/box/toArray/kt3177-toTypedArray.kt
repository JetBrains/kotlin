// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import java.util.ArrayList
import java.util.Arrays

fun box(): String {
    val list = ArrayList<Pair<String,String>>()
    list.add(Pair("Sample", "http://cyber.law.harvard.edu/rss/examples/rss2sample.xml"))
    list.add(Pair("Scripting", "http://static.scripting.com/rss.xml"))

    val keys = list.map { it.first }.toTypedArray<String>()

    val keysToString = Arrays.toString(keys)
    if (keysToString != "[Sample, Scripting]") return keysToString

    return "OK"
}
