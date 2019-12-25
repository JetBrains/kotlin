import java.util.ArrayList

fun test() {
    val abc = ArrayList<Int>()
        .map {
            it * 2
        }
        .filter {
            it > 4
        }
}

fun test1() {
    val abc = ArrayList<Int>()
        .map(
                {
                    it * 2
                },
        )
        .filter(
                {
                    it > 4
                },
        )
}

fun test2() {
    val abc = ArrayList<Int>()
        .map {
            it * 2
        }
}

fun test3() {
    val abc = ArrayList<Int>().map {
        it * 2
    }
}

fun test4() {
    val abc = ArrayList<Int>().mapTo(
            LinkedHashSet(),
    ) {
        it * 2
    }
}

fun testWithComments() {
    val abc = ArrayList<Int>()
//  .map {
//      it * 2
//  }
        .filter {
            it > 4
        }
}

// SET_TRUE: CONTINUATION_INDENT_FOR_CHAINED_CALLS
