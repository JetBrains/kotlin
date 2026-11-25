// STRING_CONCAT: indy
// JVM_TARGET: 11

fun narrow200(s: String, l: Long, d: Double): String =
        s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s

fun wide200(s: String, l: Long, d: Double): String =
        s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + l

fun wide201(s: String, l: Long, d: Double): String =
        s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + l

fun double201(s: String, l: Long, d: Double): String =
        s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s +
            s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + s + d

// 6 INVOKEDYNAMIC makeConcat\(
// 0 append
// 0 stringPlus
