// SKIP_JDK6
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

import java.util.*
import java.util.function.Predicate

class MyList : AbstractCollection<String>(), MutableCollection<String> {
    override fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun removeIf(predicate: Predicate<in String>) =
        predicate.test("abc")
}

fun box(): String {


    if (val ml = mutableListOf("xyz", "abc"); !ml.removeIf { x -> x == "abc" }) return "fail 1"
    if (val ml = mutableListOf("xyz", "abc"); ml.removeIf { x -> x == "abc" }) return "fail 2"

    if (val ml = mutableListOf("xyz", "abc"); ml != listOf("xyz")) return "fail 3"



    if (val myList = MyList(); !myList.removeIf { x -> x == "abc" }) return "fail 4"
    if (val myList = MyList(); myList.removeIf { x -> x == "xyz" }) return "fail 5"

    return "OK"
}
