public class MethodWithParameters {
    public static void foo() {
        (new k.Class()).f<caret>unction2();
    }
}

// REF: (in k.Class).function2(Byte, Char, Short, Int, Long, Boolean, Float, Double, ByteArray, CharArray, IntArray, LongArray, BooleanArray, FloatArray, DoubleArray, Array<String>, Array<ByteArray>, Array<Array<String>>, T, G, String, Class.F, Class.G, Class.F.F)
// CLS_REF: (in k.Class).function2(kotlin.Byte, kotlin.Char, kotlin.Short, kotlin.Int, kotlin.Long, kotlin.Boolean, kotlin.Float, kotlin.Double, kotlin.ByteArray, kotlin.CharArray, kotlin.IntArray, kotlin.LongArray, kotlin.BooleanArray, kotlin.FloatArray, kotlin.DoubleArray, kotlin.Array<kotlin.String>, kotlin.Array<kotlin.ByteArray>, kotlin.Array<kotlin.Array<kotlin.String>>, T, G, kotlin.String, k.Class.F, k.Class.G, k.Class.F.F)
