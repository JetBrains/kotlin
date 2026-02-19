val (Int.()-> String).valProp: String
    get() = this(1)

val <T> (Int.()-> T).valPropT: T
    get() = this(1)

fun <T> foo(): Int.() -> T {
    return { 1 as T }
}

fun bar(): Int.() -> String {
    return { with(this) { this.toString() }}
}

fun box(): String {
    foo<Int>().valPropT
    bar().valProp
    return "OK"
}