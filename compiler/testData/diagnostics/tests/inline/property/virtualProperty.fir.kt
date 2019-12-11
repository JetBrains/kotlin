final class FinalProperty {
    inline val valProp: Int
        get() = 1

    val valProp_1: Int
        inline get() = 1

    inline var varProp: Int
        get() = 1
        set(p: Int) {}

    var varProp_2: Int
        get() = 1
        inline set(p: Int) {}
}


open class OpenProperty {
    inline open val valProp: Int
        get() = 1

    open val valProp_1: Int
        inline get() = 1

    inline open var varProp: Int
        get() = 1
        set(p: Int) {}

    open var varProp_2: Int
        get() = 1
        inline set(p: Int) {}
}


interface AbstractProperty {
    inline abstract val valProp: Int
    inline abstract var varProp: Int
}
