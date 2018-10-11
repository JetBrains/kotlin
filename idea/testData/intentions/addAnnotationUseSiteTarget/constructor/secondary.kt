// IS_APPLICABLE: false

annotation class A

class Constructor {
    constructor(@A<caret> foo: String) {
    }
}