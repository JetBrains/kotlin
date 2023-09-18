
interface OV {
    val originalExpressions: A

    class Resolve<caret>Me: OV {
        override val originalExpressions: A
    }

}

class A