class C {
    var aProperty = "abc"
        get() {}
    var bProperty = "abc"
        get() {}

    fun foo(){
        $a<caret>
    }
}

// EXIST: $aProperty
// ABSENT: $bProperty
