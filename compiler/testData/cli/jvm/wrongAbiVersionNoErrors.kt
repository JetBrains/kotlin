// This should compile despite the fact that there are usages of symbols with the wrong ABI version!

import wrong.ClassWithInnerLambda

fun happy(): Int {
    return 2 + 2
}
