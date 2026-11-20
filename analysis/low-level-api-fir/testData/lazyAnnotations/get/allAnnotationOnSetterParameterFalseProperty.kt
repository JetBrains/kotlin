// QUERY: get: Property

class MyClass {
    @all:Property
    var prop: Int = 0
        set(val<caret>ue) {
            field = value
        }
}

@Target(AnnotationTarget.PROPERTY)
annotation class Property
