// ISSUE: KT-63646
// WITH_STDLIB
interface MyPropertyDelegateProvider<out Y1> {
    operator fun provideDelegate(thisRef: Nothing?, property: kotlin.reflect.KProperty<*>): Y1
}

// To look for `getValue` we need to fix Y2 after `provideDelegate` call
fun <Y2> foo(x: () -> Y2): MyPropertyDelegateProvider<Y2> = TODO()

interface MyLazy<X1> {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): X1 = TODO()
}
fun <X3> myLazy(x: () -> X3): MyLazy<X3> = TODO()

val property1 by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>foo {
    myLazy { "1" }
}<!>

val property2 by foo {
    lazy { "2" } // Regular lazy has `getValue` as an extension
}

fun main() {
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>property1<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>length<!>
    property2.length
}