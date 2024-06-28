// ISSUE: KT-68620
// WITH_STDLIB
import kotlin.test.*

var accumulator = 0

interface PropertyOwner

class Something<T : PropertyOwner>(private val ref: T) {

    val PropertyOwner.offset
        get() = 42

    private inner class Item {
        fun update(owner: T, offset: Int = owner.offset) {
            accumulator += offset
        }
    }

    fun ping() {
        val item = Item()
        item.update(ref)
    }
}

fun box(): String {
    Something(object: PropertyOwner {}).ping()
    assertEquals(42, accumulator)
    return "OK"
}
