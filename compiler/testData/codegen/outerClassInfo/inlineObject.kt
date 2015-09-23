package foo


class Foo {

    inline fun inlineFoo(crossinline s: () -> Unit) {
        val localObject = object {
            fun run() {
                s()
            }
        }

        localObject.run()
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
            val localObject = object {
                fun run() { s }
            }

            localObject.run()
        }
    }

    fun objectInLambdaInlinedIntoObject() {
        val s = 1;
        Foo().inlineFoo {
            val localObject = object {
                fun run() { s }
            }

            localObject.run()
        }
    }

}
