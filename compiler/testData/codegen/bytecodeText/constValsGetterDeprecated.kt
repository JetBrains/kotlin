const val CONST_VAL = 1

object A {
    const val CONST_VAL = 2
}

class B {
    companion object {
        const val CONST_VAL = 2
    }
}

/*
  One DEPRECATED is for INSTANCE$ temporarily
  3 others are for getCONST_VAL
*/

// 4 DEPRECATED