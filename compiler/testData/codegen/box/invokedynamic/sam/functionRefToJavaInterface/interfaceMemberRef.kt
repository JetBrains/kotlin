// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: interfaceMemberRef.kt
interface IFoo {
    fun foo(): String
}

class C(val v: String) : IFoo {
    override fun foo(): String = v
}

fun box() = Sam(IFoo::foo).get(C("OK"))

// FILE: Sam.java
public interface Sam {
    String get(IFoo c);
}
