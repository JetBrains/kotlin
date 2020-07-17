import lib.*

fun javaFun(
    <warning>g</warning>: JavaInterface.G,
    <warning>f</warning>: JavaInterface.F,
    <warning>i</warning>: JavaInterface.F.I
) {

}

fun kotlinFun(
    <warning>g</warning>: KotlinInterface.G,
    <warning>f</warning>: KotlinInterface.F,
    <warning>i</warning>: KotlinInterface.F.I
) {

}

// DEPENDENCIES: classpath:lib-classes; imports:custom.library.*