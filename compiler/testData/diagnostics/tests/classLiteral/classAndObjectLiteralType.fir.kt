import kotlin.reflect.KClass

abstract class Base<T : Any>(val klass: KClass<out T>)

class DerivedClass : Base<DerivedClass>(DerivedClass::class)

object DerivedObject : Base<DerivedObject>(DerivedObject::class)

enum class TestEnum {
    TEST_ENTRY
}

val test1: KClass<DerivedClass> = DerivedClass::class
val test2: KClass<DerivedObject> = DerivedObject::class
val test3: KClass<TestEnum> = TestEnum::class
val test4: KClass<out TestEnum> = TestEnum.TEST_ENTRY::class