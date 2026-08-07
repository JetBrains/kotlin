// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

enum class Color { RED }

@JvmInline
@JvmExposeBoxed
value class ColorBox(val color: Color)

@JvmInline
@JvmExposeBoxed
value class IntArrayBox(val array: IntArray)

@JvmExposeBoxed
fun nameOf(box: ColorBox): String = box.color.name

@JvmExposeBoxed
fun sumOf(box: IntArrayBox): Int = box.array.sum()

// FILE: Main.java
public class Main {
    public String enumBacked() {
        return ICKt.nameOf(new ColorBox(Color.RED));
    }

    public int arrayBacked() {
        return ICKt.sumOf(new IntArrayBox(new int[] { 1, 2, 3 }));
    }
}

// FILE: Box.kt
fun box(): String {
    val name = Main().enumBacked()
    if (name != "RED") return "FAIL 1: $name"
    val sum = Main().arrayBacked()
    if (sum != 6) return "FAIL 2: $sum"
    return "OK"
}
