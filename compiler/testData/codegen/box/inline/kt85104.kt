// MODULE: a
// FILE: A.kt

inline fun foo(arg: String = TODO()) = arg

// More than 32 non-extension/non-dispatch parametetrs -> more than one $mask parameter
inline fun bar(
    arg1: String = "",
    arg2: String = "",
    arg3: String = "",
    arg4: String = "",
    arg5: String = "",
    arg6: String = "",
    arg7: String = "",
    arg8: String = "",
    arg9: String = "",
    arg10: String = "",
    arg11: String = "",
    arg12: String = "",
    arg13: String = "",
    arg14: String = "",
    arg15: String = "",
    arg16: String = "",
    arg17: String = "",
    arg18: String = "",
    arg19: String = "",
    arg20: String = "",
    arg21: String = "",
    arg22: String = "",
    arg23: String = "",
    arg24: String = "",
    arg25: String = "",
    arg26: String = "",
    arg27: String = "",
    arg28: String = "",
    arg29: String = "",
    arg30: String = "",
    arg31: String = "",
    arg32: String = "",
    arg33: String = TODO(),
) = arg1

// MODULE: b(a)
// FILE: test.kt

fun box(): String {
    try {
        foo()
        return "Fail: expected NotImplementedError (1)"
    } catch (e: NotImplementedError) { }
    try {
        bar()
        return "Fail: expected NotImplementedError (2)"
    } catch (e: NotImplementedError) { }
    return "OK"
}
