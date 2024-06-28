// FIR_IDENTICAL

// FILE: FinalExternalClass.kt
package FinalExternalClass
external class ExternalClass {
    fun test(): String
    fun test(x: Int): String
    fun test(x: String): String
    fun test(vararg x: Any): String

    val test: String

    @JsName("test")
    fun notTest(): String

    @JsName("test")
    val notTest: String
}

// FILE: OpenExternalClassWithFinalMethods.kt
package OpenExternalClassWithFinalMethods
open external class ExternalClass {
    fun test(): String
    fun test(x: Int): String
    fun test(x: String): String
    fun test(vararg x: Any): String

    val test: String

    @JsName("test")
    fun notTest(): String

    @JsName("test")
    val notTest: String
}

class MyClass1 : ExternalClass() {
    fun test(x: List<Int>) = x
    fun test(vararg x: Int) = x

    fun Int.test() {}
    val Int.test get() = 1

    @JsName("test") fun notTest2() {}
}

class MyClass2 : ExternalClass() {
    fun test(x: List<Int>) = x
    fun test(vararg x: Int) = x

    fun Int.test() {}
    val Int.test get() = 1

    @JsName("test") val notTest2 = 1
}

// FILE: OpenExternalClassWithOpenMethods.kt
package OpenExternalClassWithOpenMethods
open external class ExternalClass {
    open fun test(): String
    open fun test(x: Int): String
    open fun test(x: String): String
    open fun test(vararg x: Any): String

    open val test: String

    @JsName("test")
    open fun notTest(): String

    @JsName("test")
    open val notTest: String
}

class MyClass : ExternalClass() {
    fun test(x: List<Int>) = x
    fun test(vararg x: Int) = x

    fun Int.test() {}
    val Int.test get() = 1
}

// FILE: OpenInheritedMethodClashedWithChildOverload.kt
package OpenInheritedMethodClashedWithChildOverload
open external class ExternalClass {
    <!JS_NAME_CLASH!>open fun test(x: Int): String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>fun test()<!> {}
}

// FILE: OpenInheritedMethodClashedWithChildProperty.kt
package OpenInheritedMethodClashedWithChildProperty
open external class ExternalClass {
    <!JS_NAME_CLASH!>open fun test(x: Int): String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>val test<!> = 1
}

// FILE: OpenInheritedPropertyClashedWithChildMethod.kt
package OpenInheritedPropertyClashedWithChildMethod
open external class ExternalClass {
    <!JS_NAME_CLASH!>open val test: String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>fun test()<!> {}
}

// FILE: OpenInheritedMethodClashedWithChildOverridde.kt
package OpenInheritedMethodClashedWithChildOverridde
open external class ExternalClass {
    open fun test(x: Int): Int
    <!JS_NAME_CLASH!>open fun test(x: String): String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>override fun test(x: Int)<!> = x
}

// FILE: OpenInheritedMethodClashedWithChildPropertyOverridde.kt
package OpenInheritedMethodClashedWithChildPropertyOverridde
open external class ExternalClass {
    open val test: Int
    <!JS_NAME_CLASH!>open fun test(x: String): String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>override val test<!> = 1
}

// FILE: OpenInheritedMethodClashedWithChildMethodJsName.kt
package OpenInheritedMethodClashedWithChildMethodJsName
open external class ExternalClass {
    <!JS_NAME_CLASH!>open fun test(x: String): String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>@JsName("test") fun notTest(x: String)<!> = x
}

// FILE: OpenInheritedMethodClashedWithChildPropertyJsName.kt
package OpenInheritedMethodClashedWithChildPropertyJsName
open external class ExternalClass {
    <!JS_NAME_CLASH!>open fun test(x: String): String<!>
}

class MyClass : ExternalClass() {
    <!JS_NAME_CLASH!>@JsName("test") val notTest<!> = 1
}

// FILE: OpenInheritedMethodClashedWithChildPropertyGetterJsName.kt
package OpenInheritedMethodClashedWithChildPropertyGetterJsName
open external class ExternalClass {
    <!JS_NAME_CLASH!>open fun test(x: String): String<!>
}

class MyClass : ExternalClass() {
    val notTest: Int
        <!JS_NAME_CLASH!>@JsName("test") get()<!> = 1
}

// FILE: OpenInheritedMethodClashedWithChildPropertySetterJsName.kt
package OpenInheritedMethodClashedWithChildPropertySetterJsName
open external class ExternalClass {
    <!JS_NAME_CLASH!>open fun test(x: Int): Int<!>
}

class MyClass : ExternalClass() {
    var notTest: Int
    @JsName("getterTest") get() = 1
    <!JS_NAME_CLASH!>@JsName("test") set(value)<!> { test(value) }
}

// FILE: OpenInheritedMethodClashedWithOtherInheritedMethod.kt
package OpenInheritedMethodClashedWithOtherInheritedMethod
open external class ExternalClass {
    open fun test(x: String): String
}

interface MyInterface {
    @JsName("test") fun noTest(x: Int) = 1
}

class <!JS_FAKE_NAME_CLASH!>MyClass<!> : ExternalClass(), MyInterface

// FILE: OpenInheritedMethodNotClashedWithAbstractMethod.kt
package OpenInheritedMethodNotClashedWithAbstractMethod
open external class ExternalClass {
    open fun test(): String
}

interface MyInterface {
    fun test(): String
}

class MyClass : ExternalClass(), MyInterface

// FILE: OpenInheritedMethodNotClashedWithExternalAbstractMethod.kt
package OpenInheritedMethodNotClashedWithExternalAbstractMethod
open external class ExternalClass {
    open fun test(): String
}

external interface MyInterface {
    fun test(): String
}

class MyClass : ExternalClass(), MyInterface

// FILE: OpenInheritedMethodNotClashedWithAbstractMethodWithSameName.kt
package OpenInheritedMethodNotClashedWithAbstractMethodWithSameName
open external class ExternalClass {
    open fun test(x: Int): Int
}

interface MyInterface {
    @JsName("test") fun test(x: Int): Int
}

class MyClass : ExternalClass(), MyInterface

// FILE: InheritGenericOutExternalClass.kt
package InheritGenericOutExternalClass
open external class ExternalClass<out T> {
    open fun test(x: Int?): T
    open fun test(x: Int?, y: String?): T
}

class MyClass : ExternalClass<Unit>()

// FILE: InheritGenericInExternalClass.kt
package InheritGenericInExternalClass
open external class ExternalClass<in T> {
    open fun test(x: T?): Int
    open fun test(x: T?, y: String?): Int
}

class MyClass : ExternalClass<Int>() {
    fun test(x: Int?, y: Int?) = x ?: y ?: 1
}

// FILE: InheritGenericInExternalClassClash.kt
package InheritGenericInExternalClassClash
open external class ExternalClass<in T> {
    open fun test(x: T?): Int
    <!JS_NAME_CLASH!>open fun test(x: T?, y: String?): Int<!>
}

class MyClass : ExternalClass<Int>() {
    <!JS_NAME_CLASH!>override fun test(x: Int?)<!> = x ?: 1
}
