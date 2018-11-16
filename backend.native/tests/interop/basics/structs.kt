import kotlinx.cinterop.*
import kotlin.test.*
import cstructs.*

fun main() {
    getStructWithConstFields().useContents {
        assertEquals(111, x)
        assertEquals(222, y)
    }
}