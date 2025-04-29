// ISSUE: KT-74777
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
package one

import kotlin.reflect.KProperty

abstract class Foo(var fromConstructor: String) {
    val defaultProperty = 1
    var defaultVariable = "abc"

    var customAccessorAndBackingField = "custom"
        get() = field
        set(value) {
            field = value
        }

    val customGetter
        get() = "Foo"

    var customSetter: Int = 0
        set(value) {
        }

    val customGetterDelegation by 123

    val defaultGetterWithAnnotation: String = "Foo"
        @Anno get

    var defaultSetterWithAnotherVisibility: String = "Bar"
        private set

    val Int.extensionProperty: Int get() = 0

    val postponed: Int
        get() = field

    init {
        postponed = 1
    }

    lateinit var lateinitVariable: String

    abstract val abstractProperty: Int
}

operator fun <T> T.getValue(thisRef: Any?, property: KProperty<*>): String {
    return "str"
}

annotation class Anno
