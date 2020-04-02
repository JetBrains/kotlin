annotation class MyAnn

fun bar(x: Int) {}

fun foo() {
    @MyAnn
    val x: Int
    @MyAnn
    x = @MyAnn 42
    @MyAnn
    bar(@MyAnn x)

    val y = @MyAnn if (false) x else x
    val z = @MyAnn try {
        x
    } catch (t: Throwable) {
        0
    }
}