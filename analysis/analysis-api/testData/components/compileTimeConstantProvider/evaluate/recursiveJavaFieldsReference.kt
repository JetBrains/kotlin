// FILE: JavaConst.java
public interface JavaConst {
    public static final int x = KotlinConst.y;
}

// FILE: main.kt
object KotlinConst {
    const val y = <expr>JavaConst.x</expr>;
}