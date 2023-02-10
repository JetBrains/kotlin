// ISSUE: KT-4113

class MyClass(val provider: (() -> String)?) {
    fun foo() {
        if (provider != null)
        // NI: [UNSAFE_CALL] Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type (() -> String)?
        // OI: [UNSAFE_IMPLICIT_INVOKE_CALL] Reference has a nullable type '(() -> String)?', use explicit '?.invoke()' to make a function-like call instead
            bar(provider())
    }

    fun bar(s: String) {
    }
}

class Test {
    val nullableCheckBox: B? = null
    fun fail() {
        if (nullableCheckBox != null) {
            run { nullableCheckBox() }
        }
    }
}
class B
operator fun B.invoke(): Unit = TODO()
