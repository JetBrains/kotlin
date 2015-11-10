@java.lang.Deprecated fun test1() {}

@java.lang.Deprecated fun test2() {}

fun test3() {
  @java.lang.Deprecated fun test3inner() {}
}

class Test4() {
    @java.lang.Deprecated fun test4() {}
}

class Test5() {
    fun test5() {
        @java.lang.Deprecated fun test5inner() {}
    }
}

class Test6() {
    companion object {
        @java.lang.Deprecated fun test6() {}
    }
}

object Test7 {
    @java.lang.Deprecated fun test7() {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: method:test1
// SEARCH: method:test2
// SEARCH: method:test4
// SEARCH: method:test6
// SEARCH: method:test7