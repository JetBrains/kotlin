package stopInInlineUnderSamConversion

import forTests.SamConversion

fun main(args: Array<String>) {
    val a = 1

    SamConversion.doAction({
                               inlineCall {
                                   {
                                       //Breakpoint!
                                       foo(a)
                                   }()
                               }
                           })
}

fun foo(a: Any) {}

inline fun inlineCall(f: () -> Unit) {
    f()
}