package test

enum class MyEnum {
    K;

    [inline] fun <T> doSmth(a: T) : String {
        return a.toString() + K.name()
    }
}
