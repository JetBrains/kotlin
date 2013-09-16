package first

import second.SomeTestClass

fun firstFun() {
    SomeTestClass().some<caret>
}

// EXIST: someProperty
// EXIST: someOtherProperty
// EXIST: someSelfProperty
// INVOCATION_COUNT: 3