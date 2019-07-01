// "Remove '.java'" "true"
// WITH_RUNTIME
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
fun foo() {
    bar(Foo::class.java<caret>)
}

class Foo

fun bar(kc: kotlin.reflect.KClass<Foo>) {
}