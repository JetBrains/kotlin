fun <in T> f() {
    
}

fun <out T> g() {

}

fun <out T, in X, Y> h() {

}

val <out T> T.x: Int
    get() = 1

val <in T> T.y: Int
    get() = 1