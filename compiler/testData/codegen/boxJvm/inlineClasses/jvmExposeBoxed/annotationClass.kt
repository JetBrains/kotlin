// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// Unsigned types are valid annotation member types, so an annotation class really can carry a value class.
// Annotation methods must keep their declared name and their erased return type to stay readable through
// reflection, so they are never mangled and exposure has to leave them exactly as they are.
@Retention(AnnotationRetention.RUNTIME)
@JvmExposeBoxed
annotation class Unsigned(val count: UInt, vararg val more: UInt)

@Unsigned(1u, 2u, 3u)
@JvmExposeBoxed
class Annotated(val id: Id)

// FILE: Main.java
public class Main {
    public int count() {
        return Annotated.class.getAnnotation(Unsigned.class).count();
    }

    public int moreLength() {
        return Annotated.class.getAnnotation(Unsigned.class).more().length;
    }

    public String constructor() {
        return new Annotated(new Id("OK")).getId().getValue();
    }
}

// FILE: Box.kt
fun box(): String {
    val count = Main().count()
    if (count != 1) return "FAIL 1: $count"
    val length = Main().moreLength()
    if (length != 2) return "FAIL 2: $length"
    val id = Main().constructor()
    if (id != "OK") return "FAIL 3: $id"
    return "OK"
}
