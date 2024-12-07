// FIR_IDENTICAL

// FILE: FinalClass.kt
package FinalClass
class Class {
    <!JS_NAME_CLASH!>fun test()<!> {}
    fun test(x: Int) = x
    fun test(x: String) = x
    fun test(vararg x: Any) = x

    <!JS_NAME_CLASH!>val test<!> = 0

    fun Int.test() {}
    fun Int.test(x: Int)  = x

    fun String.test() {}
    fun String.test(x: String)  = x

    val Int.test
        get() = 0

    var String.test
        get() = ""
        set(value) { test(value) }
}

// FILE: OpenClassWithFinalMethods.kt
package OpenClassWithFinalMethods
open class Class {
    fun test() {}
    fun test(x: Int) = x
    fun test(x: String) = x
    fun test(vararg x: Any) = x

    fun Int.test() {}
    fun Int.test(x: Int)  = x

    fun String.test() {}
    fun String.test(x: String)  = x
}

class MyClass1 : Class() {
    fun test(x: Char) = x

    fun Char.test() {}
    fun Char.test(x: Char)  = x
}

// FILE: OpenClassWithOpenMethods.kt
package OpenClassWithOpenMethods
open class Class {
    open fun test() {}
    open fun test(x: Int) = x
    open fun test(x: String) = x
    open fun test(vararg x: Any) = x

    open fun Int.test() {}
    open fun Int.test(x: Int)  = x

    open fun String.test() {}
    open fun String.test(x: String)  = x
}

class MyClass : Class() {
    fun test(x: List<Int>) = x
    fun test(vararg x: Int) = x

    fun Char.test() {}
    val Char.test get() = 'w'
}

// FILE: OpenInheritedMethodClashedWithChildOverload.kt
package OpenInheritedMethodClashedWithChildOverload
open class ExternalClass {
    <!JS_NAME_CLASH!>@JsName("test") open fun noTest(x: String): String<!> = x
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>fun test()<!> {}
}

// FILE: OpenInheritedMethodClashedWithChildProperty.kt
package OpenInheritedMethodClashedWithChildProperty
open class Class {
    <!JS_NAME_CLASH!>@JsName("test") open fun test(x: String): String<!> = x
}

class MyClass : Class() {
    <!JS_NAME_CLASH!>val test<!> = 1
}

// FILE: OpenInheritedPropertyClashedWithChildMethod.kt
package OpenInheritedPropertyClashedWithChildMethod
open class Class {
    <!JS_NAME_CLASH!>open val test: String<!> = ""
}

class MyClass : Class() {
    <!JS_NAME_CLASH!>fun test()<!> {}
}

// FILE: OpenInheritedMethodClashedWithChildMethodJsName.kt
package OpenInheritedMethodClashedWithChildMethodJsName
open class Class {
    <!JS_NAME_CLASH!>open fun test()<!> {}
}

class MyClass : Class() {
    <!JS_NAME_CLASH!>@JsName("test") fun notTest(x: String)<!> = x
}

// FILE: OpenInheritedMethodClashedWithChildPropertyJsName.kt
package OpenInheritedMethodClashedWithChildPropertyJsName
open class Class {
    <!JS_NAME_CLASH!>open fun test()<!> {}
}

class MyClass : Class() {
    <!JS_NAME_CLASH!>@JsName("test") val notTest<!> = 1
}

// FILE: OpenInheritedMethodClashedWithChildPropertyGetterJsName.kt
package OpenInheritedMethodClashedWithChildPropertyGetterJsName
open class Class {
    <!JS_NAME_CLASH!>open fun test()<!> {}
}

class MyClass : Class() {
    val notTest: Int
        <!JS_NAME_CLASH!>@JsName("test") get()<!> = 1
}

// FILE: OpenInheritedMethodClashedWithChildPropertySetterJsName.kt
package OpenInheritedMethodClashedWithChildPropertySetterJsName
open class Class {
    <!JS_NAME_CLASH!>open fun test()<!> {}
}

class MyClass : Class() {
    fun <T> ignore(x: T) = x

    var notTest: Int
    @JsName("getterTest") get() = 1
    <!JS_NAME_CLASH!>@JsName("test") set(value)<!> { ignore(value) }
}

// FILE: OpenInheritedMethodClashedWithOtherInheritedMethod.kt
package OpenInheritedMethodClashedWithOtherInheritedMethod
open class Class {
    open fun test() {}
}

interface MyInterface {
    @JsName("test") fun noTest(x: Int) = 1
}

class <!JS_FAKE_NAME_CLASH!>MyClass<!> : Class(), MyInterface

// FILE: OpenInheritedMethodNotClashedWithAbstractMethod.kt
package OpenInheritedMethodNotClashedWithAbstractMethod
open class Class {
    open fun test(): String = ""
}

interface MyInterface {
    fun test(): String
}

class MyClass : Class(), MyInterface

// FILE: OpenInheritedMethodNotClashedWithDefaultInterfaceMethod.kt
package OpenInheritedMethodNotClashedWithDefaultInterfaceMethod
open class Class {
    open fun test(): String = "0"
}

interface MyInterface {
    fun test(): String = "1"
}

class MyClass : Class(), MyInterface {
    override fun test(): String = "2"
}

// FILE: InterfaceImplementationWithSameJsNameClash.kt
package MultipleInterfaceInheritanceWithSameJsNameClash
interface MyInterface1 {
    @JsName("test") fun test1(): Int
}

class MyClass : MyInterface1 {
    <!JS_NAME_CLASH!>override fun test1()<!> = 1
    <!JS_NAME_CLASH!>@JsName("test") fun test2(): Int<!> = 2
}

// FILE: MultipleInterfaceImplementationWithSameJsNameClash.kt
package MultipleInterfaceImplementationWithSameJsNameClash
interface MyInterface1 {
    @JsName("test") fun test1(): Int
}

interface MyInterface2 {
    @JsName("test") fun test2(): Int
}

class MyClass : MyInterface1, MyInterface2 {
    <!JS_NAME_CLASH!>override fun test1()<!> = 1
    <!JS_NAME_CLASH!>override fun test2()<!> = 2
}
