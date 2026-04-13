// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: Processor.java

public interface Processor<T> {
    T process(T t);

    public static <T> T makePlatform(T t) {
        return t;
    }
}

// FILE: VoidProcessor.java

public interface VoidProcessor extends Processor<Void> {
    public Void process(Void t);
}

// FILE: Lib.kt

@MustUseReturnValues
class KotlinNullableProcessorImpl: VoidProcessor {
    override fun process(t: Void?): Void? {
        TODO("Not yet implemented")
    }
}

@MustUseReturnValues
class KotlinVoidProcessorImpl: VoidProcessor {
    override fun process(t: Void): Void {
        TODO("Not yet implemented")
    }
}

fun getVoid(): Void {
    TODO()
}

fun <T> useProcessor(processor: Processor<T>): T {
    return processor.process(null)
}

fun <T> id(t: T): T = t

fun test() {
    val kotlinVoidProcessorImpl = KotlinVoidProcessorImpl()
    val kotlinNullableProcessorImpl = KotlinNullableProcessorImpl()
    useProcessor(kotlinVoidProcessorImpl) // Void!
    kotlinNullableProcessorImpl.<!RETURN_VALUE_NOT_USED!>process<!>(null) // Void? is not ignorable just as Unit?
    kotlinVoidProcessorImpl.process(getVoid()) // Void

    id(Processor.makePlatform(Unit))
    id(Processor.makePlatform<Nothing?>(null))
}

// MODULE: main(lib1)

// FILE: App.kt

fun testApp() {
    val kotlinVoidProcessorImpl = KotlinVoidProcessorImpl()
    val kotlinNullableProcessorImpl = KotlinNullableProcessorImpl()
    useProcessor(kotlinVoidProcessorImpl) // Void!
    kotlinNullableProcessorImpl.<!RETURN_VALUE_NOT_USED!>process<!>(null) // Void? is not ignorable just as Unit?
    kotlinVoidProcessorImpl.process(getVoid()) // Void

    id(Processor.makePlatform(Unit))
    id(Processor.makePlatform<Nothing?>(null))
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, javaType, localProperty,
nullableType, override, propertyDeclaration, stringLiteral, typeParameter */
