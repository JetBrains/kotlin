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
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == BufferedEnum.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == UsualEnum.C<!>
    printer == CleverEnum.E
}

fun <P> processInfo2(info: String, printer: P) where P: AIPowered, P: Buffered {
    <!EQUALITY_NOT_APPLICABLE!>printer == 20<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == BufferedEnum.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == UsualEnum.C<!>
    printer == CleverEnum.E
}

abstract class Printer {
    abstract fun print(command: String)
}

fun <P> processInfo3(info: String, printer: P) where P: Buffered, P: Printer {
    <!EQUALITY_NOT_APPLICABLE!>printer == 20<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == BufferedEnum.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == UsualEnum.C<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>printer == CleverEnum.E<!>
}

fun test(a: Int, b: Any?) {
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>a === b<!>
}

fun <<!CONFLICTING_UPPER_BOUNDS, INCONSISTENT_TYPE_PARAMETER_BOUNDS, MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T: <!FINAL_UPPER_BOUND!>Int<!><!>> rest(a: T, b: Any?) where T : <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>String<!> {
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>a === b<!>
}

fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T: Any?<!>> nest(a: Int, b: T) where T : <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>String<!> {
    <!FORBIDDEN_IDENTITY_EQUALS!>a === b<!>
}

fun <<!CONFLICTING_UPPER_BOUNDS, INCONSISTENT_TYPE_PARAMETER_BOUNDS, MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T: <!FINAL_UPPER_BOUND!>Int<!><!>, <!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>K: Any?<!>> mest(a: T, b: K) where T : <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>String<!>, K: <!FINAL_UPPER_BOUND, ONLY_ONE_CLASS_BOUND_ALLOWED!>Boolean<!> {
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a === b<!>
}
