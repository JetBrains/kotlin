package util

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

abstract class AbstractClass<T>

class MyClassWithoutConstructor : @Anno("MyClassWithoutConstructor super type call $prop") AbstractClass<@Anno("MyClassWithoutConstructor nested super type ref $prop") List<@Anno("MyClassWithoutConstructor nested nested super type ref $prop") Int>>()

class MyClassWithConstructor() : @Anno("MyClassWithConstructor super type call $prop") AbstractClass<@Anno("MyClassWithConstructor nested super type ref $prop") List<@Anno("MyClassWithConstructor nested nested super type ref $prop") Int>>()