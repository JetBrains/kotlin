// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

package pack

@JvmInline
value class MyValueClass(val foo: Foo<Int>)

@JvmInline
value class Foo<T>(val a: T, val b: T)

@JvmInline
value class MyValueClass1(val foo: FooAlias)

typealias FooAlias = Foo<Int>

fun box(): String {
    val myValueClass = MyValueClass(Foo(2, 3))
    require(myValueClass.toString() == "MyValueClass(foo=${myValueClass.foo})") { myValueClass.toString() }
    require(myValueClass.toString() == "MyValueClass(foo=Foo(a=${myValueClass.foo.a}, b=${myValueClass.foo.b}))") { myValueClass.toString() }
    val myValueClass1 = MyValueClass1(FooAlias(2, 3))
    require(myValueClass1.toString() == "MyValueClass1(foo=${myValueClass.foo})") { myValueClass.toString() }
    require(myValueClass1.toString() == "MyValueClass1(foo=Foo(a=${myValueClass.foo.a}, b=${myValueClass.foo.b}))") { myValueClass.toString() }
    return "OK"
}
