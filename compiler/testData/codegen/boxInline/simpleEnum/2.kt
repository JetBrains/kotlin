package test

enum class MyEnum {
    K;

    //TODO: KT-4693
    [inline] fun <T> doSmth(a: T) : String {
        return a.toString() + K.name()
    }
}
