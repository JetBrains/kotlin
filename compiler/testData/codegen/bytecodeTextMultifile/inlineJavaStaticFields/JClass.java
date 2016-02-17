import org.jetbrains.annotations.NotNull;

public class JClass {
    public final static int PrimitiveInt = 9000;
    public final static int BigPrimitiveInt = 59000;
    public final static long PrimitiveLong = 100000;
    public final static short PrimitiveShort = 901;
    public final static boolean PrimitiveBool = false;
    public final static float PrimitiveFloat = 36.6;
    public final static double PrimitiveDouble = 42.4242;
    public final static byte PrimitiveByte = -8;
    public final static char PrimitiveChar = 'K';
    public final static String Str = ":J";

    @Nullable
    public final static String StrNullable = "nullable";

    @NotNull
    public final static Integer BoxedInt = 9500;

    public static int NonFinal = 9700;

    public final int NonStatic = 9800;
}