package test

annotation class AnnClass(val a: Class<*>)

class MyClass {

    AnnClass(javaClass<MyClass>()) default object {
    }

}
