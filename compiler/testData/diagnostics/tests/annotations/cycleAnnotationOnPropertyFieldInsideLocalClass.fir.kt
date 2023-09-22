package myPack

annotation class Anno(val number: String)

fun topLevelFun() {
    class LocalClass {
        @Anno(variableToResolve)
        @field:Anno(variableToResolve)
        var variableToResolve = "${42}"
            @Anno(variableToResolve)
            get() = field + "str"
            @Anno(variableToResolve)
            set(@Anno(variableToResolve) value) = Unit
    }
}
