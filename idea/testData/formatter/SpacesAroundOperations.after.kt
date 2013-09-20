class Some {
    fun some() {
        var int: Int = 0

        int = 12
        int += 12
        int -= 12
        int *= 12
        int /= 12
        int %= 12

        true && true
        true || false

        12 === 3
        12 !== 3
        12 == 3
        12 != 3

        12 <= 3
        12 >= 3
        12 < 3
        12 > 3

        12 + 3 - 12

        12 % 3 * 12 / 3

        1 .. 2
    }
}

// SET_TRUE: SPACE_AROUND_ASSIGNMENT_OPERATORS
// SET_TRUE: SPACE_AROUND_LOGICAL_OPERATORS
// SET_TRUE: SPACE_AROUND_EQUALITY_OPERATORS
// SET_TRUE: SPACE_AROUND_RELATIONAL_OPERATORS
// SET_TRUE: SPACE_AROUND_ADDITIVE_OPERATORS
// SET_TRUE: SPACE_AROUND_MULTIPLICATIVE_OPERATORS
// SET_TRUE: SPACE_AROUND_RANGE