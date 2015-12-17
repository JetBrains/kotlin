class Out<out T>
class OutPair<out X, out Y>
class In<in Z>
class Inv<E>

open class Open
class Final

fun skipAllOutInvWildcards(): Inv<OutPair<Open, Out<Out<Open>>>> = null!!
// method: FinalReturnTypeKt::skipAllOutInvWildcards
// generic signature: ()LInv<LOutPair<LOpen;LOut<LOut<LOpen;>;>;>;>;

fun skipAllInvWildcards(): Inv<In<Out<Open>>> = null!!
// method: FinalReturnTypeKt::skipAllInvWildcards
// generic signature: ()LInv<LIn<LOut<+LOpen;>;>;>;

fun notDeepIn(): In<Final> = null!!
// method: FinalReturnTypeKt::notDeepIn
// generic signature: ()LIn<LFinal;>;

fun skipWildcardsUntilIn0(): Out<In<Out<Open>>> = null!!
// method: FinalReturnTypeKt::skipWildcardsUntilIn0
// generic signature: ()LOut<LIn<LOut<+LOpen;>;>;>;

fun skipWildcardsUntilIn1(): Out<In<Out<Final>>> = null!!
// method: FinalReturnTypeKt::skipWildcardsUntilIn1
// generic signature: ()LOut<LIn<LOut<LFinal;>;>;>;

fun skipWildcardsUntilIn2(): Out<In<OutPair<Final, Out<Open>>>> = null!!
// method: FinalReturnTypeKt::skipWildcardsUntilIn2
// generic signature: ()LOut<LIn<LOutPair<LFinal;+LOut<+LOpen;>;>;>;>;

fun skipWildcardsUntilInProjection(): Inv<in Out<Open>> = null!!
// method: FinalReturnTypeKt::skipWildcardsUntilInProjection
// generic signature: ()LInv<-LOut<+LOpen;>;>;
