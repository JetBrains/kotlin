enum class ContentType {

    PLAIN_TEXT {
        override fun convert(text: String, targetType: ContentType): String {
            return text
        }
    },

    MARKDOWN {
        override fun convert(text: String, targetType: ContentType): String {
            return when (targetType) {
                MARKDOWN -> text
                PLAIN_TEXT -> ""
            }
        }
    };

    abstract fun convert(text: String, targetType: ContentType): String
}

fun box() =
    ContentType.PLAIN_TEXT.convert("OK", ContentType.PLAIN_TEXT)
