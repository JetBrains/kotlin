// DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNUSED_PARAMETER

interface AssertionPlant<out T : Any>
interface AssertionPlantNullable<out T : Any?>: BaseAssertionPlant<T, AssertionPlantNullable<T>>
interface BaseAssertionPlant<out T : Any?, out A : BaseAssertionPlant<T, A>>

interface BaseCollectingAssertionPlant<out T, out A : BaseAssertionPlant<T, A>, out C : BaseCollectingAssertionPlant<T, A, C>> : BaseAssertionPlant<T, A>

interface CreatorLike<TSubject, T, A : BaseAssertionPlant<T, A>, C : BaseCollectingAssertionPlant<T, A, C>>

interface ParameterObjectOption {
    fun <TSubject : Any, T : Any?> withParameterObjectNullable(
        parameterObject: ParameterObject<TSubject, T>
    ) = null as CreatorNullable<TSubject, T>
}

class ParameterObject<TSubject, T>
interface CollectingAssertionPlantNullable<out T> : AssertionPlantNullable<T>,
    BaseCollectingAssertionPlant<T, AssertionPlantNullable<T>, CollectingAssertionPlantNullable<T>>
interface CreatorNullable<TSubject, T>: CreatorLike<TSubject, T, AssertionPlantNullable<T>, CollectingAssertionPlantNullable<T>>

fun <K, V, M, A : BaseAssertionPlant<V, A>, C : BaseCollectingAssertionPlant<V, A, C>> contains(
    pairs: List<Pair<K, M>>,
    parameterObjectOption: (ParameterObjectOption, K) -> CreatorLike<Map<out K, V>, V, A, C>,
    assertionCreator: C.(M) -> Unit
) {}

private fun <K, V> createGetParameterObject(
    plant: AssertionPlant<Map<out K, V>>,
    key: K
) = null as ParameterObject<Map<out K, V>, V>

private fun <K, V : Any, M> containsNullable(
    plant: AssertionPlant<Map<out K, V?>>,
    pairs: List<Pair<K, M>>,
    assertionCreator: AssertionPlantNullable<V?>.(M) -> Unit
) = contains(
    pairs,
    { option, key -> option.withParameterObjectNullable(createGetParameterObject(plant, key)) },
    assertionCreator
)
