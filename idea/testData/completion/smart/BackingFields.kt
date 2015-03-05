class C {
    var property1 = "abc" // has backing field but accessors are default - no sense to show in completion

    var property2: String // no backing field
        get() = "abc"
        set(value){}

    var property3 = "abc" // has backing field and accessor
        get() = $property3 + 1

    var property4 = 1
        get() = $property4 + 1

    var property5: String? = null
        get() = $property3 + 1

    fun foo(): String {
        return <caret>
    }
}

// ABSENT: $property1
// ABSENT: $property2
// EXIST: $property3
// ABSENT: $property4
// EXIST: { itemText: "!! $property5" }
// EXIST: { itemText: "?: $property5" }
