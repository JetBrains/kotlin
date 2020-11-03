// IGNORE_BACKEND_FIR: JVM_IR
// LANGUAGE: +InlineClasses
import kotlin.reflect.KProperty

inline class I(val x: Int)

interface A {
    val i: I
}

class Delegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): I {
        return I(1)
    }
}

class B : A {
    override val i by Delegate()
}

// 1 public final getValue-Y6jMyTM\(Ljava/lang/Object;Lkotlin/reflect/KProperty;\)I
// 1 public getI-lPtA-2M\(\)I
// 1 public abstract getI-lPtA-2M\(\)I
