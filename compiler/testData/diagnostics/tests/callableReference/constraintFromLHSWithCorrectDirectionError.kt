// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// DIAGNOSTICS: -UNUSED_PARAMETER

package test
import kotlin.reflect.KProperty1

interface A {
    val bla: CharSequence get() = ""
}

class B<T>(val x: T)
fun <K, V> B<K>.foo(p: KProperty1<K, V>) {}

class C : A

fun <R : A> B<R>.test(){
    foo(<!TYPE_MISMATCH, TYPE_MISMATCH!>C::bla<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, interfaceDeclaration,
nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
