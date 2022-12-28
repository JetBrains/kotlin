// WITH_EXTENDED_CHECKERS

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>kotlin.jvm.functions.Function0<!>

val x: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Function0<Int><!> = <!INITIALIZER_TYPE_MISMATCH!>{ 42 }<!>

val y: Function1<String, String> = { it }

class MyFunction : Function2<Int, String, Unit> {
    override fun invoke(p1: Int, p2: String) {}
}
