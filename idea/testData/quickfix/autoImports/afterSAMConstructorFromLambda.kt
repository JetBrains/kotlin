// "Import" "true"
import java.util.Collections
import java.util.ArrayList
import java.util.Comparator

fun foo() {
    Collections.sort(
            ArrayList<Int>(),
            <caret>Comparator { x: Int, y: Int -> x - y }
    )
}