class B<M>

interface A<T, Y : B<T>, T_I1: T> {

    fun <T, L> p(p: T): T {
        return p
    }

}

// method: A$DefaultImpls::p
// jvm signature: (LA;Ljava/lang/Object;)Ljava/lang/Object;
// generic signature: <T_I2:Ljava/lang/Object;Y:LB<TT_I2;>;T_I1::TT_I2;T:Ljava/lang/Object;L:Ljava/lang/Object;>(LA<TT_I2;TY;TT_I1;>;TT;)TT;
