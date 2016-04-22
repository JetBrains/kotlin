// WITH_RUNTIME
// IS_APPLICABLE: false
import java.util.ArrayList

var globalCollection = ArrayList<Int>()

fun foo(list: List<Collection<Int>>) {
    <caret>for (collection in list) {
        globalCollection.add(collection.size)
    }
}