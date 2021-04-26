
fun <T> foo1(x: T!!, y: List<T!!>, z: T!!.(T!!) -> T!!): T!! {}

// should be prohibited on type-resolution level
fun <T> foo2(x: T!!?, y: List<T!!?>, z: T!!?.(T!!?) -> T!!?, w: String!!): T!! {}

fun foo3() {
    if (@RetentionSourceAndTargetExpression !!(x == y)) {}
}
