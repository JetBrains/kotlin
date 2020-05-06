import cunion.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main() {
    memScoped {
        val basicUnion = alloc<BasicUnion>()
        for (value in Short.MIN_VALUE..Short.MAX_VALUE) {
            basicUnion.ll = value.toLong()
            assertEquals(value.toShort(), basicUnion.s)
        }
    }
    memScoped {
        val struct = alloc<StructWithUnion>()
        struct.`as`.i = Float.NaN.toRawBits()
        assertEquals(Float.NaN, struct.`as`.f)
    }
    memScoped {
        val union = alloc<Packed>()
        union.b = 1u
        assertEquals(1u, union.i)
        union.i = 0u
        assertEquals(0u, union.b)
    }
}