inline class SomeClass(val v: Int) {
    companion object {
        fun <T> comp(s: SomeClass, t: T): T? = null
    }

    fun <T> getT(): T? = null

    fun <T, K> getTK(t: T): K? = null

    val <K> K.propK: K? get() = null
}

// method: SomeClass::getT-impl
// jvm signature: (I)Ljava/lang/Object;
// generic signature: <T:Ljava/lang/Object;>(I)TT;

// method: SomeClass::getTK-impl
// jvm signature: (ILjava/lang/Object;)Ljava/lang/Object;
// generic signature: <T:Ljava/lang/Object;K:Ljava/lang/Object;>(ITT;)TK;

// method: SomeClass::getPropK-impl
// jvm signature: (ILjava/lang/Object;)Ljava/lang/Object;
// generic signature: <K:Ljava/lang/Object;>(ITK;)TK;

// method: SomeClass$Companion::comp-hRy0JnA
// jvm signature: (ILjava/lang/Object;)Ljava/lang/Object;
// generic signature: <T:Ljava/lang/Object;>(ITT;)TT;