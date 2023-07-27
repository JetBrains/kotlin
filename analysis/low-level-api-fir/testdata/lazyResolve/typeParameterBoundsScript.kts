fun resolve<caret>Me(foo: Foo) {
    foo.util()
}

interface Foo
interface Bar<T : Foo>

fun <F : Foo, B : Bar<F>> F.util(): B = null!!