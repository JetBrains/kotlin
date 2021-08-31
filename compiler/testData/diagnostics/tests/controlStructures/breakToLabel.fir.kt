//KT-48116
//WITH_STDLIB

fun foo() {
    var tokenType: String? = null
    while (true) {
        FindTagEnd@ while (tokenType.let { it != null && it !== "XML_END_TAG_START" }) {
            if (tokenType === "XML_COMMENT_CHARACTERS") {
                // we should terminate on first occurence of </
                val end = tokenType
                for (i in tokenType) {
                    if (i == ' ') {
                        break@FindTagEnd
                    }
                }
            }
            tokenType = "abc"
        }
    }
}