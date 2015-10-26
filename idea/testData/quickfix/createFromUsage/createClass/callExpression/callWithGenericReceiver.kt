// "Create class 'Foo'" "true"
// ERROR: Type inference failed: constructor Foo<U>(u: U)<br>cannot be applied to<br>(U)<br>
// ERROR: Type mismatch: inferred type is U but U was expected

class A<T>(val n: T) {

}

fun test<U>(u: U) {
    val a = A(u).<caret>Foo(u)
}