package a

class A

class Cl {
    companion object {
        @JvmField val property1 = A()
        const val property2 = 2
    }
}

interface Int {
    companion object {
        val property1 = A()
        const val property2 = 2
    }
}

object Obj {
    @JvmField val property1 = A()
    const val property2 = 2
}