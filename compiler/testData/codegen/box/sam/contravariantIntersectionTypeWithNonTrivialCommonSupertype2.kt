// TARGET_BACKEND: JVM

// TODO support different bytecode text templates for FIR?
// --CHECK_BYTECODE_TEXT
// --JVM_IR_TEMPLATES
// --2 java/lang/invoke/LambdaMetafactory

// FILE: test.kt

interface Top
interface Unrelated

interface A : Top, Unrelated
interface B : Top, Unrelated

fun box(): String {
    // TODO: https://youtrack.jetbrains.com/issue/KT-46238
    val version = System.getProperty("java.specification.version")
    if (version != "1.6" && version != "1.8") return "OK"

    val g = when ("".length) {
        0 -> G<A>()
        else -> G<B>()
    }

    g.check {}
    g.check(::functionReference)
    return "OK"
}

fun functionReference(x: Any) {}

class G<T : Top> {
    fun check(x: IFoo<in T>) {
        x.accept(object : A {} as T)
    }
}

// FILE: IFoo.java

public interface IFoo<T extends Top> {
    void accept(T t);
}
