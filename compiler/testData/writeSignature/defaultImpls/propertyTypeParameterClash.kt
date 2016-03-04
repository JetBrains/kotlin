class B<M>

interface A<T, Y : B<T>> {

    val <T> T.z: T?
        get() = null
}

// method: A$DefaultImpls::getZ
// jvm signature: (LA;Ljava/lang/Object;)Ljava/lang/Object;
// generic signature: <T_I1:Ljava/lang/Object;Y:LB<TT_I1;>;T:Ljava/lang/Object;>(LA<TT_I1;TY;>;TT;)TT;
