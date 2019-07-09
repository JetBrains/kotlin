// CLASS_NAME_SUFFIX: A$foo$Local

import kotlin.reflect.KClass

class A {
    annotation class Ann(val info: String)

    annotation class Bnn(val klass: KClass<*>)

    fun foo() {
        @Ann("class")
        class Local {
            @Ann("fun")
            fun foo(): Local = this

            @field:Ann("val")
            @Bnn(Local::class)
            val x = foo()

            @Ann("inner")
            @Bnn(Array<Array<out Local>>::class)
            inner class Inner
        }
    }
}
