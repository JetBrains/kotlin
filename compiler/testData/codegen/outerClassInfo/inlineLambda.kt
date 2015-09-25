package foo


class Foo {

    inline fun inlineFoo(crossinline s: () -> Unit) {
        {
            s()
        }()
    }

    inline fun simpleFoo(s: () -> Unit) {
        s()
    }
}


class Bar {
    fun callToInline() {
        Foo().inlineFoo { 1 }
    }

    fun objectInInlineLambda() {
        val s = 1;
        Foo().simpleFoo {
            {
                s
            }()
        }
    }

    fun objectInLambdaInlinedIntoObject() {
        val s = 1;
        Foo().inlineFoo {
            {
                s
            }()
        }
    }

}
