interface Buffered {
    fun flush()
}

interface AIPowered {
    fun getAvatarReleaseYear(): Int
}

enum class BufferedEnum : Buffered {
    A, B;
    override fun flush() {}
}

enum class UsualEnum {
    C, D;
}

enum class CleverEnum : Buffered, AIPowered {
    E, F;
    override fun flush() {}
    override fun getAvatarReleaseYear() = 2022
}

fun <P> processInfo1(info: String, printer: P) where P: Buffered, P: AIPowered {
    <!EQUALITY_NOT_APPLICABLE!>printer == 20<!>
    printer == BufferedEnum.A
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>printer == UsualEnum.C<!>
    printer == CleverEnum.E
}

fun <P> processInfo2(info: String, printer: P) where P: AIPowered, P: Buffered {
    <!EQUALITY_NOT_APPLICABLE!>printer == 20<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>printer == BufferedEnum.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>printer == UsualEnum.C<!>
    printer == CleverEnum.E
}

abstract class Printer {
    abstract fun print(command: String)
}

fun <P> processInfo3(info: String, printer: P) where P: Buffered, P: Printer {
    <!EQUALITY_NOT_APPLICABLE!>printer == 20<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>printer == BufferedEnum.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>printer == UsualEnum.C<!>
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>printer == CleverEnum.E<!>
}
