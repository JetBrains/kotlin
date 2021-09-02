// JSR305_GLOBAL_REPORT: warn

// FILE: J.java

public class J {
    @MyNonnull
    public static J staticNN;
    @MyNullable
    public static J staticN;
    public static J staticJ;

    public static void staticSet(@MyNonnull J nn, @MyNullable J n, J j) {}

    public J(@MyNonnull J nn, @MyNullable J n, J j) {}
    public J() {}

    @MyNonnull
    public J nn;
    @MyNullable
    public J n;
    public J j;

    public void set(@MyNonnull J nn, @MyNullable J n, J j) {}
}

// FILE: k.kt
fun test(n: J?, nn: J) {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    J.staticNN = n
    J.staticNN = platformN
    J.staticNN = nn
    J.staticNN = platformNN
    J.staticNN = platformJ

    J.staticN = n
    J.staticN = platformN
    J.staticN = nn
    J.staticN = platformNN
    J.staticN = platformJ

    J.staticJ = n
    J.staticJ = platformN
    J.staticJ = nn
    J.staticJ = platformNN
    J.staticJ = platformJ

    J.staticSet(nn, nn, nn)
    J.staticSet(platformNN, platformNN, platformNN)
    J.staticSet(n, n, n)
    J.staticSet(platformN, platformN, platformN)
    J.staticSet(platformJ, platformJ, platformJ)

    J().nn = n
    J().nn = platformN
    J().nn = nn
    J().nn = platformNN
    J().nn = platformJ

    J().n = n
    J().n = platformN
    J().n = nn
    J().n = platformNN
    J().n = platformJ

    J().j = n
    J().j = platformN
    J().j = nn
    J().j = platformNN
    J().j = platformJ

    J().set(nn, nn, nn)
    J().set(platformNN, platformNN, platformNN)
    J().set(n, n, n)
    J().set(platformN, platformN, platformN)
    J().set(platformJ, platformJ, platformJ)

    J(nn, nn, nn)
    J(platformNN, platformNN, platformNN)
    J(n, n, n)
    J(platformN, platformN, platformN)
    J(platformJ, platformJ, platformJ)
}
