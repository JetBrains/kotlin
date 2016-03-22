@MyAnnotation("f", "s") fun test1() {}

@MyAnnotation("f", "s") class Test1() {}

@MyAnnotation("f", "s") val test3 = 1

annotation class MyAnnotation(val first: String, val second: String)

// ANNOTATION: MyAnnotation
// SEARCH: method:test1
// SEARCH: class:Test1
// SEARCH: field:test3