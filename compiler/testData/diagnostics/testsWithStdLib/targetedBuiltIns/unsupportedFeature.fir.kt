// !CHECK_TYPE
// !LANGUAGE: -AdditionalBuiltInsMembers
// SKIP_TXT
// FULL_JDK

class A : java.util.ArrayList<String>() {
    // `stream` is defined in ArrayList, so it was impossible to override it in 1.0
    override fun stream(): java.util.stream.Stream<String> = super.stream()

    // `sort` is defined in ArrayList, so it was possible to override it in 1.0
    override fun sort(c: Comparator<in String>?) {
        super.sort(c)
    }
}

class A1 : java.util.ArrayList<String>() {
    // `stream` is defined in Collection, so it was possible to declare it in 1.0 without an 'override' keyword
    fun stream(): java.util.stream.Stream<String> = super.stream()

    // `sort` is defined in ArrayList, so it was impossible to declare it in 1.0 without an 'override' keyword
    fun <!VIRTUAL_MEMBER_HIDDEN!>sort<!>(c: Comparator<in String>?) {
        super.sort(c)
    }
}

interface A2 : List<String> {
    override fun stream(): java.util.stream.Stream<String> = null!!
}

class B : <!NONE_APPLICABLE!>Throwable<!>("", null, false, false)

class B1 : RuntimeException() {
    override fun fillInStackTrace(): Throwable { // 'override' keyword must be prohibited, as it was in 1.0.x
        return this
    }
}

class A3(val x: List<String>) : List<String> by x

fun Throwable.fillInStackTrace() = 1

fun foo(x: List<String>, y: Throwable, z: A3) {
    x.stream()
    java.util.ArrayList<String>().stream()

    y.fillInStackTrace() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Int>() }

    HashMap<String, Int>().getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>Any()<!>, null)

    // Falls back to extension in stdlib
    y.printStackTrace()

    z.stream()
}

interface X {
    fun foo(): Int = 1
    val hidden: Boolean
}

class Y : X {
    // There should not be UNSUPPORTED_FEATURE diagnostic
    override fun foo() = 1
    override var hidden: Boolean = true
}
