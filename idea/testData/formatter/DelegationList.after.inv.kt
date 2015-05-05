interface A1
interface A2
interface A3

open class B1

class Simpleclass1() : B1(), A1,
        A2, A3

class SimpleClass2() :
        B1(), A1,
        A2, A3

class Temp {
    class SimpleClass3() : B1(), A1,
            A2, A3
}

object SimpleObject1 : B1(), A1,
        A2, A3

object SimpleObject2 : B1(),
        A1,
        A2, A3

open class C1
open class C2

enum class Test : A1 {
    FIRST_ITEM : Test()
    SECOND_ITEM :
            Test()
}

// SET_TRUE: ALIGN_MULTILINE_EXTENDS_LIST