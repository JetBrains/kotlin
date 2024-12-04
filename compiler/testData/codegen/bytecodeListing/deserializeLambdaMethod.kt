// FILE: deserializeLambdaMethod.kt

fun plusK(s: String) = s + "K"

// Creating a serializable lambda causes '$deserializeLambda$' method to be generated in the corresponding class.
// Given equivalent code, javac generates
//      private static synthetic $deserializeLambda$(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;
val test = Sam(::plusK)

// FILE: Sam.java
import java.io.*;

public interface Sam extends Serializable {
    String get(String s);
}
