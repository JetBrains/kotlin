// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>Base1<!> {
    fun foo(a: String): String
}

expect interface <!NO_ACTUAL_FOR_EXPECT!>Base2<!> {
    fun foo(a: Any): Any
}

expect interface <!NO_ACTUAL_FOR_EXPECT!>Base3<!> {
    fun foo(a: String): String
}

class Test1(val a : Base2): Base1, Base2 by a {
    override fun foo(a: String): String {
        return a
    }
}

class Test2(val a: Base1): Base3, Base1 by a

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base1 {
    actual fun foo(a: String): String
}

actual interface Base2 {
    actual fun foo(a: Any): Any
}

actual interface Base3 {
    actual fun foo(a: String): String
}
class Base1Impl : Base1 {
    override fun foo(a: String): String {
        return a
    }
}

class Base2Impl : Base2 {
    override fun foo(a: Any): Any {
        return 1
    }
}

open class Base3Impl : Base3 {
    override fun foo(a: String): String {
        return a
    }
}

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class Test3<!>(val a: Base1): Base3Impl(), Base1 by a

fun test(){
    Test1(Base2Impl()).foo("")
    Test1(Base2Impl()).foo(1)

    Test2(Base1Impl()).foo("")

    Test3(Base1Impl()).foo("")
}