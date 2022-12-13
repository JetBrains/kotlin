
interface OV {
    val originalExpressions: A

    class ResolveMe: OV {
        override val originalExpressions: A
    }

}

class A