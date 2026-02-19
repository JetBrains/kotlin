// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-78866

interface Foo<T> {
    fun bar()
}
context(foo: Foo<T>) fun <T> bar() = foo.bar()

interface FooA : Foo<String>
interface FooB : Foo<Int>
interface FooC<R> : Foo<R>

fun test1(a: FooA, b: FooB) {
    with(a) {
        context(b) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>bar<!>()
        }
    }
}

fun test2(a: FooC<Int>, b: FooC<String>) {
    with(a) {
        context(b) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>bar<!>()
        }
    }
}

fun <A, B> test3(a: FooC<A>, b: FooC<B>) {
    with(a) {
        context(b) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>bar<!>()
        }
    }
}

fun Foo<String>.onlyForString() { }
context(foo: Foo<String>) fun onlyForString() { }

fun test4(a: FooC<String>, b: FooC<String>) {
    with(a) {
        context(b) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>onlyForString<!>()
        }
    }
}

fun test5(a: FooC<Int>, b: FooC<String>) {
    with(a) {
        context(b) {
            onlyForString()
        }
    }
}

fun <A> select(x: A, y: A) { }

fun test6(a: FooC<String>, b: FooC<String>) {
    with(a) {
        context(b) {
            select("x", <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>onlyForString<!>())
        }
    }
}

fun test7(a: FooC<Int>, b: FooC<String>) {
    with(a) {
        context(b) {
            select(1, onlyForString())
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, typeParameter */
