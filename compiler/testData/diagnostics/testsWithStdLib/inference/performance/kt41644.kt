// FIR_IDENTICAL
//!DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS

sealed class DataType<T> {
    sealed class NotNull<T> : DataType<T>() {
        abstract class Partial<T> : NotNull<T>()
    }
}

class Tuple8<A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>>(
    firstName: String, firstType: DA,
    secondName: String, secondType: DB,
    thirdName: String, thirdType: DC,
    fourthName: String, fourthType: DD,
    fifthName: String, fifthType: DE,
    sixthName: String, sixthType: DF,
    seventhName: String, seventhType: DG,
    eighthName: String, eighthType: DH
) : Schema<Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>()

class EitherType<SCH : Schema<SCH>>(
    schema: SCH
)

open class Schema<T>

fun <A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>, E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>> either8(
    firstName: String, firstType: DA,
    secondName: String, secondType: DB,
    thirdName: String, thirdType: DC,
    fourthName: String, fourthType: DD,
    fifthName: String, fifthType: DE,
    sixthName: String, sixthType: DF,
    seventhName: String, seventhType: DG,
    eighthName: String, eighthType: DH
): DataType.NotNull.Partial<Either8<A, B, C, D, E, F, G, H>> =
    EitherType(
        Tuple8(
            firstName, firstType, secondName, secondType, thirdName, thirdType, fourthName, fourthType,
            fifthName, fifthType, sixthName, sixthType, seventhName, seventhType, eighthName, eighthType
        )
    ) as DataType.NotNull.Partial<Either8<A, B, C, D, E, F, G, H>>

class Either8<T, U, V, W, X, Y, Z, T1>
