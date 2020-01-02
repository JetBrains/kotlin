package test
import kotlin.reflect.KClass

annotation class AnnClass(val a: KClass<*>)

class MyClass {

    @AnnClass(MyClass::class)
    companion object {
    }

}
