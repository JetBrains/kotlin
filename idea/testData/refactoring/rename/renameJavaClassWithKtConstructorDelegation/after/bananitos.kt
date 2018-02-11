class DoesntWork : Banana2 {
    constructor() : super()
    constructor(f: () -> String) : super(f)

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val inst = DoesntWork {"Hi there"}
            inst.goCrazy()
        }
    }
}

class ThisWorks(f: () -> String) : Banana2(f) {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val inst = ThisWorks {"Hi there"}
            inst.goCrazy()
        }
    }
}