package util

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

abstract class AbstractClass<T>

class MyClass : @Anno("super type call $prop") AbstractClass<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") Int>> {
    constructor(): super() {

    }
}
