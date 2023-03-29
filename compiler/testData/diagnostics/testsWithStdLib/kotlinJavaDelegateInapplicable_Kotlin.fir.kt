// ISSUE: KT-57543
// RENDER_DIAGNOSTICS_MESSAGES

class KotlinClass {

    class Val<T>(private final val initializer: Function0<T>) {
        operator fun getValue(instance: Any, metadata: Any): T {
            return initializer.invoke()
        }
    }

    companion object {
        fun <T> lazySoft(initializer: Function0<T>): Val<T> {
            return Val<T>(initializer)
        }
    }
}

class A(
    val c: Int? = 0,
    myType: (() -> Int)? = null
) {
    val arguments: A by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE("getValue(A, KProperty1<A, A>); [fun getValue(instance: Any, metadata: Any): Stub (chain inference): TypeVariable(_T)]"), NEW_INFERENCE_ERROR("NewConstraintError at Incorporate TypeVariable(_K) == kotlin/Nothing? from Fix variable TypeVariable(_K) from position Fix variable TypeVariable(_K): kotlin/Function0<TypeVariable(__R)> <!: kotlin/Nothing?")!>KotlinClass.lazySoft {
        <!NEW_INFERENCE_ERROR("NewConstraintError at Incorporate TypeVariable(_K) == kotlin/Nothing? from Fix variable TypeVariable(_K) from position Fix variable TypeVariable(_K): kotlin/Function0<TypeVariable(__R)> <!: kotlin/Nothing?")!>A(myType = if (false) null else fun(): Int { return <!NEW_INFERENCE_ERROR("NewConstraintError at Incorporate TypeVariable(_K) == kotlin/Nothing? from Fix variable TypeVariable(_K) from position Fix variable TypeVariable(_K): kotlin/Function0<TypeVariable(__R)> <!: kotlin/Nothing?")!>c!!<!> })<!>
    }<!>
}
