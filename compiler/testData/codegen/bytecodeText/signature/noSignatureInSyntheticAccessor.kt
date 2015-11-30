class MyList<T> {

    private fun noSignature(): T? = null

    fun withSignature(): T? = null

    fun removeHeader() {
        fun a () {
            noSignature()
        }
    }
}

/*
Class signature,
local fun class signature,
'noSignature' and 'withSignature' fun signatures
 */

// 4 signature
// 2 signature \(\)TT\;
// 1 signature Lkotlin/jvm/internal/Lambda\;Lkotlin/jvm/functions/Function0<Lkotlin/Unit\;>\;
// 1 signature <T:Ljava/lang/Object\;>Ljava/lang/Object\;
// 1 public final static synthetic access\$noSignature\(LMyList\;\)Ljava/lang/Object