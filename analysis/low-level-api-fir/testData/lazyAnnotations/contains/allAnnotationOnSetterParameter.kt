// QUERY: contains: Param

class MyClass {
    @all:Param
    var prop: Int = 0
        set(val<caret>ue) {
            field = value
        }
}

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param
