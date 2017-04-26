class Outer {
    inner class Inner<G>

    fun <A> foo(): Inner<A> = TODO()
}

// method: Outer::foo
// jvm signature: ()LOuter$Inner;
// generic signature: <A:Ljava/lang/Object;>()LOuter$Inner<TA;>;