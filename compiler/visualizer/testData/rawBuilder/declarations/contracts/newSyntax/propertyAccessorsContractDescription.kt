// FIR_IGNORE
// new contracts syntax for property accessors
class MyClass {
//      Int          Int
//      │            │
    var myInt: Int = 0
//                                          Int
//                                          │
        get() contract [returnsNotNull()] = 1
//      Int
//      │
    set(value) {
//      var MyClass.<set-myInt>.field: Int
//      │       MyClass.<set-myInt>.value: Int
//      │       │     fun (Int).times(Int): Int
//      │       │     │ Int
//      │       │     │ │
        field = value * 10
    }
}

class AnotherClass(multiplier: Int) {
//      Int               Int
//      │                 │
    var anotherInt: Int = 0
//                                          Int
//                                          │
        get() contract [returnsNotNull()] = 1
//      Int
//      │
    set(value) contract [returns()] {
//      var AnotherClass.<set-anotherInt>.field: Int
//      │       AnotherClass.<set-anotherInt>.value: Int
//      │       │     [ERROR: not resolved]
//      │       │     │ [ERROR: not resolved]
//      │       │     │ │
        field = value * multiplier
    }
}

class SomeClass(multiplier: Int?) {
//      Int            Int
//      │              │
    var someInt: Int = 0
//                                          Int
//                                          │
        get() contract [returnsNotNull()] = 1
//      Int                                          [ERROR: unknown type]
//      │                                            │
    set(value) contract [returns() implies (value != null)] {
//      SomeClass.<set-someInt>.value: Int
//      │              [ERROR: not resolved]
//      │              │
        value ?: throw NullArgumentException()
//      var SomeClass.<set-someInt>.field: Int
//      │       SomeClass.<set-someInt>.value: Int
//      │       │
        field = value
    }
}
