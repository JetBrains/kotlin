// PROBLEM: none
class MyClass

inline fun <T> acceptMyClass(m: (MyClass?) -> T) {}

fun one(): MyClass?<caret> {
    acceptMyClass { return it }
    return MyClass()
}