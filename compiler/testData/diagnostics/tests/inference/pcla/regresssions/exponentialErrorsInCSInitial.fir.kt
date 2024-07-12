// ISSUE: KT-65812
// WITH_STDLIB

fun <R> scope(block: () -> R): R = block()

public fun <E> List<E>.permutations1(k: Int = size) {
    val collection = this

    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR!>sequence<!> {
        if (collection.size < k) return@sequence

        val size = collection.size
        val references = <!UNRESOLVED_REFERENCE!>MutableList234<!>(size + 1) { <!UNRESOLVED_REFERENCE!>it<!> + 1 }.<!CANNOT_INFER_PARAMETER_TYPE!>apply<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
            <!CANNOT_INFER_PARAMETER_TYPE!>this<!>[1] = k+1
            for (t in 1..k) <!CANNOT_INFER_PARAMETER_TYPE!>this<!>[t] = 1
        }<!>
        val currentIndices = IntArray(<!ARGUMENT_TYPE_MISMATCH!>k<!>) <!ARGUMENT_TYPE_MISMATCH!>{ it + 1 }<!>
        val currentElements = <!UNRESOLVED_REFERENCE!>MutableList234<!>(k) { collection[<!UNRESOLVED_REFERENCE!>it<!>] }

        fun addStartMark(): Int {
            val index = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!BUILDER_INFERENCE_STUB_RECEIVER!>references<!>[<!ARGUMENT_TYPE_MISMATCH!>1<!>]<!>
            <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!BUILDER_INFERENCE_STUB_RECEIVER!>references<!>[<!ARGUMENT_TYPE_MISMATCH!>1<!>]<!> = <!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!BUILDER_INFERENCE_STUB_RECEIVER!>references<!>[<!ARGUMENT_TYPE_MISMATCH!>index<!>]<!>
            <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!BUILDER_INFERENCE_STUB_RECEIVER!>references<!>[<!ARGUMENT_TYPE_MISMATCH!>index<!>]<!> = <!ARGUMENT_TYPE_MISMATCH!>1<!>
            return index
        }
        fun removeMark(index: Int) {

        }
        fun moveToNextMark(index: Int): Int {
            TODO()
        }

        while (true) {
            <!INAPPLICABLE_CANDIDATE!>yield<!>(currentElements.toList())

            val firstToIncrease = <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>scope<!> <!ARGUMENT_TYPE_MISMATCH!>{
                var current = k - 1
                var index = currentIndices[current]
                while (<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!BUILDER_INFERENCE_STUB_RECEIVER!>references<!>[<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><!BUILDER_INFERENCE_STUB_RECEIVER!>references<!>[<!ARGUMENT_TYPE_MISMATCH!>index<!>]<!>]<!> == size + 1) {
                    removeMark(index)
                    current--
                    if (current == Int.MAX_VALUE) break
                    index = currentIndices[current]
                }
                current
            }<!>
            if (firstToIncrease == Int.MAX_VALUE) return@sequence

            val newIndex = moveToNextMark(<!ARGUMENT_TYPE_MISMATCH!>currentIndices[<!ARGUMENT_TYPE_MISMATCH!>firstToIncrease<!>]<!>)
            currentIndices[<!ARGUMENT_TYPE_MISMATCH!>firstToIncrease<!>] = <!ARGUMENT_TYPE_MISMATCH!>newIndex<!>
            <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>currentElements[firstToIncrease]<!> = collection[newIndex-1]

            for (t in <!ITERATOR_MISSING!>firstToIncrease+1 .. <!ARGUMENT_TYPE_MISMATCH!>k<!><!>) {
                val index = addStartMark()
                currentIndices[t] = index
                currentElements[t] = collection[index-1]
            }
        }
    }
}
