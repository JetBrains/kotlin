// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES
// !WITH_ENUM_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 6
 DESCRIPTION: 'When' with exhaustive when expression in the control structure body.
 */

fun case_1(value: Int, value1: Int, value2: Boolean?, value3: _EnumClass?, value4: _SealedClass?) {
    when {
        value == 1 -> when {
            value1 > 1000 -> "1"
            value1 > 100 -> "2"
            value1 > 10 || value1 < -10 -> "3"
            else -> "4"
        }
        value == 2 -> when(value2!!) {
            true -> "1"
            false -> "2"
        }
        value == 3 -> when(value2) {
            true -> "1"
            false -> "2"
            null -> "3"
        }
        value == 4 -> when(value3!!) {
            _EnumClass.WEST -> "1"
            _EnumClass.SOUTH -> "2"
            _EnumClass.NORTH -> "3"
            _EnumClass.EAST -> "4"
        }
        value == 5 -> when(value3) {
            _EnumClass.WEST -> "1"
            _EnumClass.SOUTH -> "2"
            _EnumClass.NORTH -> "3"
            _EnumClass.EAST -> "4"
            null -> "5"
        }
        value == 6 -> when(value4!!) {
            is _SealedChild1 -> "1"
            is _SealedChild2 -> "2"
            is _SealedChild3 -> "3"
        }
        value == 7 -> {
            when(value4) {
                is _SealedChild1 -> "1"
                is _SealedChild2 -> "2"
                is _SealedChild3 -> "3"
                null -> "4"
            }
        }
    }
}