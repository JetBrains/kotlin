// WITH_STDLIB
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class A {
    constructor(param: @Anno("parameter type $prop") List<@Anno("nested parameter type $prop") Collection<@Anno("nested nested parameter type $prop") String>> = @Anno("defaultValue $prop") fun(i: @Anno("anonymousFunction parameter type $prop") Int): @Anno("anonymousFunction return type $prop") Int {})
}

abstract class AbstractClass<T>

class MyClass : @Anno("super type call $prop") AbstractClass<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") Int>>()

class MyClassWithConstructor() : @Anno("super type call $prop") AbstractClass<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") Int>>()

interface I

class MyClassWithSuperInterface : @Anno("super type ref $prop") List<@Anno("nested super type ref $prop") List<@Anno("nested nested super type ref $prop") I>>
