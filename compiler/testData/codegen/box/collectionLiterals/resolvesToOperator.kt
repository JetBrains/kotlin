// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +CollectionLiterals

enum class MyList {
    OK, MEMBER_FUN, EXTENSION_FUN;

    companion object {
        operator fun of(vararg es: CharSequence): MyList = OK
        fun of(vararg es: String): MyList = MEMBER_FUN
    }
}

fun MyList.Companion.of(es: CharSequence): MyList = MyList.EXTENSION_FUN

fun box(): String {
    var failure: String? = null
    fun myAssert(actual: MyList, expected: MyList, onFailure: () -> String) {
        if (actual != expected) failure = onFailure()
    }

    myAssert(MyList.of(""), MyList.MEMBER_FUN) {
        """MyList.of("")"""
    }
    myAssert(MyList.of("" as CharSequence), MyList.OK) {
        """MyList.of("" as CharSequence)"""
    }

    val colLitNoArg: MyList = []
    val colLitCS: MyList = ["" as CharSequence]
    val colLitString: MyList = [""]

    myAssert(colLitNoArg, MyList.OK) { "[]" }
    myAssert(colLitCS, MyList.OK) { """["" as CharSequence]"""}
    myAssert(colLitString, MyList.OK) { """[""]"""}

    return failure ?: "OK"
}
