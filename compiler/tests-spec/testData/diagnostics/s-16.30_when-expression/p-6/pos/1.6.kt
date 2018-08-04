// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_ENUM_CLASSES
// !WITH_SEALED_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 6
 DESCRIPTION: 'When' with exhaustive when expression in the control structure body.
 */

fun case_1(value: Int, value1: Int, value2: Boolean?, value3: _EnumClass?, value4: _SealedClass?) {
    when (value) {
        1 -> when {
            value1 > 1000 -> "1"
            value1 > 100 -> "2"
            value1 > 10 || value1 < -10 -> "3"
            else -> "4"
        }
        2 -> when(value2!!) {
            true -> "1"
            false -> "2"
        }
        3 -> when(value2) {
            true -> "1"
            false -> "2"
            null -> "3"
        }
        4 -> when(value3!!) {
            _EnumClass.WEST -> "1"
            _EnumClass.SOUTH -> "2"
            _EnumClass.NORTH -> "3"
            _EnumClass.EAST -> "4"
        }
        5 -> when(value3) {
            _EnumClass.WEST -> "1"
            _EnumClass.SOUTH -> "2"
            _EnumClass.NORTH -> "3"
            _EnumClass.EAST -> "4"
            null -> "5"
        }
        6 -> when(value4!!) {
            is _SealedChild1 -> "1"
            is _SealedChild2 -> "2"
            is _SealedChild3 -> "3"
        }
        7 -> {
            when(value4) {
                is _SealedChild1 -> "1"
                is _SealedChild2 -> "2"
                is _SealedChild3 -> "3"
                null -> "4"
            }
        }
    }
}