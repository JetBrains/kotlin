// DIAGNOSTICS: -UNUSED_VARIABLE

fun test(boolean: Boolean) {
    val expectedLong: Long = if (boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>if (boolean) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>42<!>
        } else {
            return
        }<!>
    } else {
        return
    }

    val expectedInt: Int = if (boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (boolean) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>42<!>
        } else {
            return
        }<!>
    } else {
        return
    }

    val expectedShort: Short = if (boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short")!>if (boolean) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Short")!>42<!>
        } else {
            return
        }<!>
    } else {
        return
    }

    val expectedByte: Byte = if (boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte")!>if (boolean) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte")!>42<!>
        } else {
            return
        }<!>
    } else {
        return
    }
}
