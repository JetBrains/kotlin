// ISSUE: KT-65812
// WITH_STDLIB

fun <R> scope(block: () -> R): R = block()

public fun <E> List<E>.permutations1(k: Int = size) {
    val collection = this

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>sequence<!> {
        if (collection.size < k) return@sequence

        val size = collection.size
        val references = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>MutableList234<!>(size + 1) { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>it<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> 1 }.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>apply<!> {
            <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>this<!NO_SET_METHOD!>[1]<!><!> = k+1
            for (t in 1..k) <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>this<!NO_SET_METHOD!>[t]<!><!> = 1
        }
        val currentIndices = IntArray(k) { it + 1 }
        val currentElements = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>MutableList234<!>(k) { collection[<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>it<!>] }

        fun addStartMark(): Int {
            val index = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>references<!>[1]<!>
            <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>references<!>[1]<!> = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>references<!>[<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>index<!>]<!>
            <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>references<!>[<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>index<!>]<!> = 1
            return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>index<!>
        }
        fun removeMark(index: Int) {

        }
        fun moveToNextMark(index: Int): Int {
            TODO()
        }

        while (true) {
            yield(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>currentElements<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>toList<!>())

            val firstToIncrease = scope {
                var current = k - 1
                var index = currentIndices[current]
                while (<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>references<!>[<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>references<!>[index]<!>]<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> size + 1) {
                    removeMark(index)
                    current--
                    if (current == Int.MAX_VALUE) break
                    index = currentIndices[current]
                }
                current
            }
            if (firstToIncrease == Int.MAX_VALUE) return@sequence

            val newIndex = moveToNextMark(currentIndices[firstToIncrease])
            currentIndices[firstToIncrease] = newIndex
            <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>currentElements<!>[firstToIncrease]<!> = collection[newIndex-1]

            for (t in firstToIncrease+1 .. k) {
                val index = addStartMark()
                currentIndices[t] = index
                <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>currentElements<!>[t]<!> = collection[index-1]
            }
        }
    }
}
