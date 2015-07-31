MyAnnotation("f", "s") fun test1() {}

MyAnnotation("f", "s") class Test1() {}

MyAnnotation("f", "s") val test3 = 1

annotation class MyAnnotation(val first: String, val second: String)

// ANNOTATION: MyAnnotation
// SEARCH: KotlinLightMethodForDeclaration:test1
// SEARCH: KotlinLightClass:Test1