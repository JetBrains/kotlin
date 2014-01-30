fun foo() {
    val (i :Int, s :String) = bar()
    val h = {() :Unit -> bar() }
    for (i :Int in collection) {
    }
}

// SET_FALSE: SPACE_BEFORE_TYPE_COLON
// SET_TRUE: SPACE_AFTER_TYPE_COLON