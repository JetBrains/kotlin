// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// Only 'reified' type parameters make an inline function impossible to expose; a plain inline function keeps
// its callable JVM declaration, so Java can call the boxed variant and pass a Function1.
@JvmExposeBoxed
inline fun transform(id: Id, f: (Id) -> Id): Id = f(id)

// FILE: Main.java
import kotlin.jvm.functions.Function1;

public class Main {
    public String test() {
        return ICKt.transform(new Id("O"), new Function1<Id, Id>() {
            @Override
            public Id invoke(Id id) {
                return new Id(id.getValue() + "K");
            }
        }).getValue();
    }
}

// FILE: Box.kt
fun box(): String = Main().test()
