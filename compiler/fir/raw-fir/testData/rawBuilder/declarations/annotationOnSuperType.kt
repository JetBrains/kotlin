package util

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

interface I

class MyClassWithoutConstructor : @Anno("MyClassWithoutConstructor super type ref $prop") List<@Anno("MyClassWithoutConstructor nested super type ref $prop") List<@Anno("MyClassWithoutConstructor nested nested super type ref $prop") I>>

class MyClassWithConstructor() : @Anno("MyClassWithConstructor super type ref $prop") List<@Anno("MyClassWithConstructor nested super type ref $prop") List<@Anno("MyClassWithConstructor nested nested super type ref $prop") I>>