abstract class Outer {
    inner class FirstInner {
        inner class SecondInner<A> {
            inner class ThirdInnner {
                inner class FourthInner<B>
            }
        }
    }

    fun <T, V> foo(): FirstInner.SecondInner<T>.ThirdInnner.FourthInner<V> = TODO()
}

// method: Outer::foo
// jvm signature: ()LOuter$FirstInner$SecondInner$ThirdInnner$FourthInner;
// generic signature: <T:Ljava/lang/Object;V:Ljava/lang/Object;>()LOuter$FirstInner$SecondInner<TT;>.ThirdInnner.FourthInner<TV;>;