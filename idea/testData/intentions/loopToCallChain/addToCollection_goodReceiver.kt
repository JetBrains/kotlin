// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapTo(){}'"
import java.util.ArrayList

fun foo(list: List<MutableCollection<Int>>) {
    var target: MutableCollection<Int>()
    target = ArrayList<Int>()

    <caret>for (collection in list) {
        target.add(collection.size)
    }

    if (target.size > 100) {
        target = ArrayList<Int>()
    }
}