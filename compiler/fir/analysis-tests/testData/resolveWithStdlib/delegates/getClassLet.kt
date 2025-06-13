// RUN_PIPELINE_TILL: BACKEND
import kotlin.reflect.KClass

class SomeClass

inline fun <reified K> foo(klass: KClass<*>): K = null!!

val some: Map<String, String> by lazy {
    SomeClass::class.let {
        foo(it)
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, classReference, functionDeclaration, inline, lambdaLiteral,
nullableType, propertyDeclaration, propertyDelegate, reified, starProjection, typeParameter */
