var globalProperty = "abc" // has backing field and accessor but backing field is not accessible
    get() = $globalProperty + 1

class C {
    var property1 = "abc" // has backing field but accessors are default - no sense to show in completion

    var property2 = "abc" // has backing field but accessors are also default
        private set

    var property3: String // no backing field
        get() = "abc"
        set(value){}

    var property4 = "abc" // has backing field and accessor
        get() = $property4 + 1

    fun foo(){
        <caret>
    }
}

// ABSENT: $globalProperty
// ABSENT: $property1
// ABSENT: $property2
// ABSENT: $property3
// EXIST: { lookupString: "$property4", itemText: "$property4", tailText: null, typeText: "String" }
