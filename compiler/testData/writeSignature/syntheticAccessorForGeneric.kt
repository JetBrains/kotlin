class MyList<T> {

    private fun noSignature(): T? = null

    fun withSignature(): T? = null

    fun removeHeader() {
        fun a () {
            noSignature()
        }
    }
}

// method: MyList::withSignature
// jvm signature: ()Ljava/lang/Object;
// generic signature: ()TT;

// method: MyList::access$noSignature
// jvm signature: (LMyList;)Ljava/lang/Object;
// generic signature: null
