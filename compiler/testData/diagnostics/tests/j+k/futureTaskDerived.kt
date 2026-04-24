// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK

import java.util.concurrent.FutureTask

class MyTask<M>(val runnable: Runnable, value: M?) : FutureTask<M>(runnable, value)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
