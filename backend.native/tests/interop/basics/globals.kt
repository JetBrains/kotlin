import kotlinx.cinterop.*
import cglobals.*

fun main(args: Array<String>) {
    assert(g1__ == 42)

    assert(g2 == 17)
    g2 = 42
    assert(g2 == 42)

    assert(g3.x == 128)
    g3.x = 7
    assert(g3.x == 7)

    assert(g4[1] == 14)
    g4[1] = 15
    assert(g4[1] == 15)

    assert(g5[0] == 15)
    assert(g5[3] == 18)
    g5[0] = 16
    assert(g5[0] == 16)

    assert(g6 == g3.ptr)
}
