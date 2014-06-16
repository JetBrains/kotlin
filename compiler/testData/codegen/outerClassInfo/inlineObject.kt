package foo;

import kotlin.InlineOption.*

class Foo {

    inline fun inlineFoo(inlineOptions(ONLY_LOCAL_RETURN) s: () -> Unit) {
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