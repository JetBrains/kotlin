// LAMBDAS: CLASS

public fun <T> Iterable<T>.myforEach(operation: (T) -> Unit) : Unit {
    for (element in this) operation(element)
}

public fun println(v: Any?) {}

fun foo1() {
    (1..5).myforEach {
        println(it)
    }
}

// METHOD : ItInLambdaKt$foo1$1.invoke(I)V
// VARIABLE : NAME=this TYPE=LItInLambdaKt$foo1$1; INDEX=0
// VARIABLE : NAME=it TYPE=I INDEX=1
