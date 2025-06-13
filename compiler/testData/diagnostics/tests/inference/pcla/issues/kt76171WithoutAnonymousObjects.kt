// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76171
import kotlin.reflect.KProperty

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun myRun(b: () -> Unit) {}
fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun awaitResult(
    state: FakeMutableState<String>
) {
    generate {
        myRun {
            var expectedValue by state
        }
        yield(state.value)
    }
}

class FakeMutableState<T>(var value: T) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate, setter, starProjection, suspend,
typeParameter, typeWithExtension */
