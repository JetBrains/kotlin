// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-65812
// WITH_STDLIB

fun <R> scope(block: () -> R): R = block()

public fun <E> List<E>.permutations1(k: Int = size) {
    val collection = this

    <!CANNOT_INFER_PARAMETER_TYPE!>sequence<!> {
        if (collection.size < k) return@sequence

        val size = collection.size
        val references = <!UNRESOLVED_REFERENCE!>MutableList234<!>(size + 1) { <!UNRESOLVED_REFERENCE!>it<!> + 1 }.apply <!CANNOT_INFER_RECEIVER_PARAMETER_TYPE!>{
            <!CANNOT_INFER_RECEIVER_PARAMETER_TYPE!>this<!>[1] = k+1
            for (t in 1..k) <!CANNOT_INFER_RECEIVER_PARAMETER_TYPE!>this<!>[t] = 1
        }<!>
        val currentIndices = IntArray(<!ARGUMENT_TYPE_MISMATCH!>k<!>) <!ARGUMENT_TYPE_MISMATCH!>{ it + 1 }<!>
        val currentElements = <!UNRESOLVED_REFERENCE!>MutableList234<!>(k) { collection[<!UNRESOLVED_REFERENCE!>it<!>] }

        fun addStartMark(): Int {
            val index = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>references[1]<!>
            references<!NO_SET_METHOD!>[1]<!> = references[<!ARGUMENT_TYPE_MISMATCH!>index<!>]
            references<!NO_SET_METHOD!>[index]<!> = 1
            return <!RETURN_TYPE_MISMATCH!>index<!>
        }
        fun removeMark(index: Int) {

        }
        fun moveToNextMark(index: Int): Int {
            TODO()
        }

        while (true) {
            <!INAPPLICABLE_CANDIDATE!>yield<!>(currentElements.toList())

            val firstToIncrease = <!CANNOT_INFER_PARAMETER_TYPE!>scope<!> <!ARGUMENT_TYPE_MISMATCH!>{
                var current = k - 1
                var index = currentIndices[current]
                while (<!EQUALITY_NOT_APPLICABLE!>references[<!ARGUMENT_TYPE_MISMATCH!>references[<!ARGUMENT_TYPE_MISMATCH!>index<!>]<!>] == size + 1<!>) {
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
            currentElements[firstToIncrease] = collection[newIndex-1]

            for (t in <!ITERATOR_MISSING!>firstToIncrease+1 .. <!ARGUMENT_TYPE_MISMATCH!>k<!><!>) {
                val index = addStartMark()
                currentIndices[t] = index
                currentElements[t] = collection[index-1]
            }
        }
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, break, comparisonExpression, equalityExpression, forLoop,
funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, incrementDecrementExpression,
integerLiteral, lambdaLiteral, localFunction, localProperty, nullableType, propertyDeclaration, rangeExpression,
thisExpression, typeParameter, whileLoop */
