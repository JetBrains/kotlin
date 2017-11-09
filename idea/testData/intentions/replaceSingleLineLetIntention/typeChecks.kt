// IS_APPLICABLE: false
// WITH_RUNTIME

interface Base

class A : Base
class B : Base

fun isAB(arg: Base) = arg.let<caret> { it is A || it is B }