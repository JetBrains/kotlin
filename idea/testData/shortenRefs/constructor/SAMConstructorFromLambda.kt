import java.util.Collections
import java.util.ArrayList

fun foo() {
    Collections.sort(
            ArrayList<Int>(),
            <selection>java.util.Comparator</selection> { x: Int, y: Int -> x - y }
    )
}