fun x() {
    val xProperty = 10
    fun xFunction() = 11
    typealias xTypealias = 10
    class XClass<XT> {}
    enum class XEnum {
        X_ENUM_ENTRY;

        fun xEnumMember(){}
    }

    class Y <YT> {
        val yProperty = 10
        fun yFunction() = 11
        typealias yTypealias = 10
        class YClass<YTT> {}
        enum class YEnum {
            Y_ENUM_ENTRY;

            fun yEnumMember(){}

        }
        init {
            println("init block in local class")
        }
    }
}