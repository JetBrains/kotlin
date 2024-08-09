// FILE: JavaA.java
public abstract class JavaA<T> {
    public T javaField = null;
}



// FILE: KotlinB.kt
abstract class KotlinB<T>(s: String) : JavaA<T>() {
    constructor(t: T) : this(t.toString())

    abstract val prop: T
    abstract fun func(): T
}

// FILE: JavaC.java
class JavaC extends KotlinB<Integer> {
    public JavaC(Integer integer) {
        super(integer);
    }

    @Override
    public Integer func() {
        return 0;
    }

    @Override
    public Integer getProp() {
        return 0;
    }
}

// class: JavaC
